/*
 * This file is part of faker - https://github.com/o1seth/faker
 * Copyright (C) 2024 o1seth
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
#include <winsock2.h>
#include <windows.h>
#include <ctype.h>
#include <stdio.h>
#include <stdlib.h>
#include <ws2tcpip.h>
#include <winsock.h>
#define WINDIVERTEXPORT
#include <windivert.h>
#define MAXBUF          WINDIVERT_MTU_MAX
#define MDNS_PRIORITY 96
#define TTL_PRIORITY 97
#define IP_BLOCK_PRIORITY 98
#define PORT_FORWARD_PRIORITY 99
#define REDIRECT_PRIORITY 100
char LOG_LEVEL = 2;
#define DEBUG_LEVEL 1
#define INFO_LEVEL 2
#define WARN_LEVEL 3
BOOL is_debug() {
	return LOG_LEVEL != 0 && LOG_LEVEL <= DEBUG_LEVEL;
}
BOOL is_info() {
	return LOG_LEVEL != 0 && LOG_LEVEL <= INFO_LEVEL;
}
BOOL is_warn() {
	return LOG_LEVEL != 0 && LOG_LEVEL <= WARN_LEVEL;
}
__declspec(dllexport) void set_log_level(char level) {
	LOG_LEVEL = level;
}

typedef struct
{
	HANDLE handle;
	HANDLE thread_handle;
	UINT32 SrcIp;
	UINT16 SrcPort;
	UINT32 DstIp;
	UINT16 DstPort;
	UINT32 NewDstIp;
	UINT16 NewDstPort;
	UINT32 FakeSrcIp;
	UINT64 LastPacketTime;
	UINT32 syn_AckNum;
	UINT32 syn_SeqNum;

	UINT32 win_nat_SrcIp;
	UINT16 win_nat_SrcPort;

	BOOL server_fin;
	UINT32 server_fin_ack;
	UINT32 server_fin_seq;
	BOOL client_fin;
	UINT32 client_fin_ack;
	UINT32 client_fin_seq;
	BOOL closed;
	BOOL releasing;
	int index;
	void* owner;
	BOOL should_free;
} TCP_CONNECTION, * PTCP_CONNECTION;

typedef struct
{
	HANDLE forward_handle;
	HANDLE network_handle;

	HANDLE connection_lock;
	HANDLE thread_in;
	HANDLE thread_clean;
	BOOL cleanup_thread_interrupted;

	SOCKET socket;
	UINT16 binded_port;//port for fake incoming connection from 127.x.x.x to 127.0.0.1
	UINT16 redirect_port;

	UINT32 skip_local_ports_count;
	UINT32 skip_local_ports_size;
	UINT16* skip_local_ports;

	char layer;
	char* buf;
	unsigned char* packet;
	UINT32 connection_count;
	UINT32 connections_size;
	PTCP_CONNECTION* connections;
	UINT32 next_ip;
	BOOL pause;
} REDIRECT, * PREDIRECT;

typedef struct
{
	HANDLE forward_handle;
	HANDLE network_handle;
	HANDLE thread;
	unsigned char* packet;
} TTL_FIX, * PTTL_FIX;
typedef struct
{
	HANDLE windivert_handle;
	HANDLE thread;
	unsigned char* packet;
} MDNS, * PMDNS;

typedef struct
{
	UINT16 DstPort;//in ntohs, uses only for read and compare

	UINT32 SrcIp;
	UINT16 SrcPort;
	char protocol;//IPPROTO_TCP, IPPROTO_UDP
	UINT16 local_port;
	SOCKET socket;

	UINT64 LastPacketTime;
	UINT32 syn_AckNum;
	UINT32 syn_SeqNum;

	BOOL syn;
	BOOL server_fin;
	UINT32 server_fin_ack;
	UINT32 server_fin_seq;
	BOOL client_fin;
	UINT32 client_fin_ack;
	UINT32 client_fin_seq;
	BOOL closed;
	BOOL releasing;
	int index;
	void* owner;
	BOOL should_free;
} PORT_FORWARD, * PPORT_FORWARD;

typedef struct
{
	char* buf;
	char* packet;

	UINT16* skip_ports;
	UINT16 skip_ports_count;

	HANDLE windivert;

	UINT32 forward_to_addr;
	UINT32 this_machine_addr;
	UINT32 listen_addr;
	UINT32 listen_start_addr;
	UINT32 listen_end_addr;

	HANDLE thread_in;

	UINT32 tcp_port_count;
	UINT32 udp_port_count;

	UINT32 connection_count;
	UINT32 connections_size;
	PPORT_FORWARD* connections;

	UINT64 last_clean_time;
} FORWARD, * PFORWARD;

typedef struct
{
	char* packet;
	HANDLE windivert;
	HANDLE thread_in;
} BLOCK_IP, * PBLOCK_IP;


static void message(FILE* const f, const char* msg, ...)
{
	static HANDLE lock = 0;
	if (!lock) {
		lock = CreateMutex(NULL, FALSE, NULL);
	}
	va_list args;
	va_start(args, msg);
	if (lock != 0) {
		WaitForSingleObject(lock, INFINITE);
	}
	vfprintf(f, msg, args);
	putc('\n', f);
	if (lock != 0) {
		ReleaseMutex(lock);
	}
	fflush(f);
	va_end(args);
}
char error_buf[1024];
#define error(msg, ...)								\
	sprintf(error_buf,msg,__VA_ARGS__);
#define warning(msg, ...)							\
if(is_warn())										\
	message(stderr, msg, ## __VA_ARGS__)
#define info(msg, ...)								\
if(is_info())										\
	message(stdout, msg, ## __VA_ARGS__)
#define debug(msg, ...)								\
if(is_debug())										\
	message(stdout, msg, ## __VA_ARGS__)


__declspec(dllexport)char* get_error() {
	return error_buf;
}

__declspec(dllexport)void reset_error() {
	error_buf[0] = 0;
	error_buf[1] = 0;
}

UINT32 ip4(char* ip) {
	struct in_addr addr;
	inet_pton(AF_INET, ip, &addr);
	return addr.s_addr;
}
void print_iphdr(PWINDIVERT_IPHDR hdr)
{
	info("HdrLength: %lu", hdr->HdrLength);
	info("Version: %lu", hdr->Version);
	info("TOS: %lu", hdr->TOS);
	info("Length: %lu", ntohs(hdr->Length));
	info("Id: %lu", ntohs(hdr->Id));
	info("FragOff0: %lu", hdr->FragOff0);
	info("TTL: %lu", hdr->TTL);
	info("Protocol: %lu", hdr->Protocol);
	info("Checksum: %lu", hdr->Checksum);

	struct in_addr addr;
	addr.s_addr = hdr->SrcAddr;
	info("SrcAddr %s (%lu)", inet_ntoa(addr), hdr->SrcAddr);
	addr.s_addr = hdr->DstAddr;
	info("DstAddr %s (%lu)", inet_ntoa(addr), hdr->DstAddr);
}

void print_tcphdr(PWINDIVERT_TCPHDR hdr)
{
	info(" SrcPort: %lu", ntohs(hdr->SrcPort));
	info(" DstPort: %lu", ntohs(hdr->DstPort));
	info(" SeqNum: %lu", ntohl(hdr->SeqNum));
	info(" AckNum: %lu", ntohl(hdr->AckNum));
	info(" HdrLength: %lu", ntohs(hdr->HdrLength));
	info(" Fin: %lu", hdr->Fin);
	info(" Syn: %lu", hdr->Syn);
	info(" Rst: %lu", hdr->Rst);
	info(" Psh: %lu", hdr->Psh);
	info(" Ack: %lu", hdr->Ack);
	info(" Urg: %lu", hdr->Urg);
	info(" Reserved2: %lu", hdr->Reserved2);
	info(" Window: %lu", ntohs(hdr->Window));
	info(" Checksum: %lu", ntohs(hdr->Checksum));
	info(" UrgPtr: %lu", ntohs(hdr->UrgPtr));
}

void print_udphdr(PWINDIVERT_UDPHDR hdr)
{
	info(" SrcPort: %lu", ntohs(hdr->SrcPort));
	info(" DstPort: %lu", ntohs(hdr->DstPort));
	info(" Checksum: %lu", ntohl(hdr->Checksum));
	info(" Length: %lu", ntohl(hdr->Length));
}

void print_icmphdr(PWINDIVERT_ICMPHDR hdr)
{
	info(" Body: %lu", hdr->Body);
	info(" Checksum: %lu", ntohs(hdr->Checksum));
	info(" Code: %lu", ntohl(hdr->Code));
	info(" Type: %lu", ntohl(hdr->Type));
}

void print_addr(PWINDIVERT_ADDRESS addr)
{
	info("    Timestamp: %llu", addr->Timestamp);
	info("    Layer: %lu", addr->Layer);
	info("    Event: %lu", addr->Event);
	info("    Sniffed? %lu", addr->Sniffed);
	info("    Outbound? %lu", addr->Outbound);
	info("    Loopback? %lu", addr->Loopback);
	info("    Impostor? %lu", addr->Impostor);
	info("    IPv6? %lu", addr->IPv6);
	info("    Valid IPChecksum? %lu", addr->IPChecksum);
	info("    Valid TCPChecksum? %lu", addr->TCPChecksum);
	info("    Valid UDPChecksum? %lu", addr->UDPChecksum);
	info("    IfIdx: %lu", addr->Network.IfIdx);
	info("    SubIfIdx: %lu", addr->Network.SubIfIdx);
}
char* get_tcp_flag(PWINDIVERT_TCPHDR tcp_header) {
	if (tcp_header->Syn && tcp_header->Ack) {
		return "SYN+ACK";
	}
	if (tcp_header->Fin && tcp_header->Ack) {
		return "FIN+ACK";
	}
	if (tcp_header->Psh && tcp_header->Ack) {
		return "PSH+ACK";
	}
	if (tcp_header->Rst && tcp_header->Ack) {
		return "RST+ACK";
	}
	if (tcp_header->Syn) {
		return "SYN";
	}
	if (tcp_header->Ack) {
		return "Ack";
	}
	if (tcp_header->Fin) {
		return "Fin";
	}
	if (tcp_header->Rst) {
		return "Rst";
	}
	if (tcp_header->Psh) {
		return "Psh";
	}
	if (tcp_header->Urg) {
		return "Urg";
	}
	return "";
}
void sprint_tcp_src_dst(PWINDIVERT_IPHDR ip_header, PWINDIVERT_TCPHDR tcp_header, PWINDIVERT_ADDRESS addr, char* dest) {
	char src[64];
	char dst[64];
	struct in_addr src_addr;
	src_addr.s_addr = ip_header->SrcAddr;
	inet_ntop(AF_INET, &src_addr, src, sizeof(src));

	struct in_addr dst_addr;
	dst_addr.s_addr = ip_header->DstAddr;
	inet_ntop(AF_INET, &dst_addr, dst, sizeof(dst));

	if (addr->Outbound) {
		sprintf(dest, "tcp (out) %s:%d -> %s:%d\0", src, ntohs(tcp_header->SrcPort), dst, ntohs(tcp_header->DstPort));
	}
	else {
		sprintf(dest, "tcp (in ) %s:%d -> %s:%d\0", src, ntohs(tcp_header->SrcPort), dst, ntohs(tcp_header->DstPort));
	}
}

void sprint_udp_src_dst(PWINDIVERT_IPHDR ip_header, PWINDIVERT_UDPHDR udp_header, PWINDIVERT_ADDRESS addr, char* dest) {
	char src[64];
	char dst[64];
	struct in_addr src_addr;
	src_addr.s_addr = ip_header->SrcAddr;
	inet_ntop(AF_INET, &src_addr, src, sizeof(src));

	struct in_addr dst_addr;
	dst_addr.s_addr = ip_header->DstAddr;
	inet_ntop(AF_INET, &dst_addr, dst, sizeof(dst));

	if (addr->Outbound) {
		sprintf(dest, "udp (out) %s:%d -> %s:%d\0", src, ntohs(udp_header->SrcPort), dst, ntohs(udp_header->DstPort));
	}
	else {
		sprintf(dest, "udp (in ) %s:%d -> %s:%d\0", src, ntohs(udp_header->SrcPort), dst, ntohs(udp_header->DstPort));
	}
}

void sprint_packet(PWINDIVERT_IPHDR ip_header, PWINDIVERT_ADDRESS addr, char* dest) {
	char src[64];
	char dst[64];
	struct in_addr src_addr;
	src_addr.s_addr = ip_header->SrcAddr;
	inet_ntop(AF_INET, &src_addr, src, sizeof(src));

	struct in_addr dst_addr;
	dst_addr.s_addr = ip_header->DstAddr;
	inet_ntop(AF_INET, &dst_addr, dst, sizeof(dst));

	if (addr->Outbound) {
		if (addr->Impostor) {
			sprintf(dest, "packet (out, impostor, if %lu) %s -> %s\0", addr->Network.IfIdx, src, dst);
		}
		else {
			sprintf(dest, "packet (out, if %lu) %s -> %s\0", addr->Network.IfIdx, src, dst);
		}
	}
	else {
		if (addr->Impostor) {
			sprintf(dest, "packet (in , impostor, if %lu) %s -> %s\0", addr->Network.IfIdx, src, dst);
		}
		else {
			sprintf(dest, "packet (in , if %lu) %s -> %s\0", addr->Network.IfIdx, src, dst);
		}
	}
}

char* ntoa(UINT32 ip) {
	struct in_addr addr;
	addr.s_addr = ip;
	return inet_ntoa(addr);
}
void print_packet_src_dst(PWINDIVERT_IPHDR ip_header, PWINDIVERT_TCPHDR tcp_header, PWINDIVERT_ADDRESS addr) {
	struct in_addr src_addr;
	src_addr.s_addr = ip_header->SrcAddr;
	if (addr->Outbound) {
		info("packet (out) %s:%d (%lu)->", inet_ntoa(src_addr), ntohs(tcp_header->SrcPort), ip_header->SrcAddr);
	}
	else {
		info("packet (in ) %s:%d (%lu)->", inet_ntoa(src_addr), ntohs(tcp_header->SrcPort), ip_header->SrcAddr);
	}
	struct in_addr dst_addr;
	dst_addr.s_addr = ip_header->DstAddr;
	info("%s:%d (%lu)", inet_ntoa(dst_addr), ntohs(tcp_header->DstPort), ip_header->DstAddr);
}

void print_packet(PWINDIVERT_IPHDR ip_header, PWINDIVERT_ADDRESS addr) {
	struct in_addr src_addr;
	src_addr.s_addr = ip_header->SrcAddr;
	if (addr->Outbound) {
		info("packet (out) %s (%lu)->", inet_ntoa(src_addr), ip_header->SrcAddr);
	}
	else {
		info("packet (in ) %s (%lu)->", inet_ntoa(src_addr), ip_header->SrcAddr);
	}
	struct in_addr dst_addr;
	dst_addr.s_addr = ip_header->DstAddr;
	info("%s: (%lu)", inet_ntoa(dst_addr), ip_header->DstAddr);
}

PTCP_CONNECTION newConnection(PREDIRECT redirect) {
	PTCP_CONNECTION connection = calloc(1, sizeof(TCP_CONNECTION));
	if (connection == 0) {
		error("Failed to allocate new connection");
		return 0;
	}
	connection->owner = redirect;
	connection->LastPacketTime = GetTickCount64();
	connection->should_free = TRUE;
	HANDLE lock = redirect->connection_lock;
	WaitForSingleObject(lock, INFINITE);
	UINT32 index = redirect->connection_count;

	if (index >= redirect->connections_size) {
		UINT32 newSize = redirect->connections_size / 2 * 3;
		PTCP_CONNECTION* newConnections = realloc(redirect->connections, newSize * sizeof(PTCP_CONNECTION));
		if (newConnections == 0) {
			error("Failed to reallocate new connection %d -> %d", redirect->connections_size, newSize);
			return 0;
		}
		redirect->connections_size = newSize;
		redirect->connections = newConnections;
	}
	redirect->connections[index] = connection;
	connection->index = index;

	redirect->connection_count = index + 1;
	ReleaseMutex(lock);
	return connection;
}

void freeConnection(PREDIRECT redirect, PTCP_CONNECTION c) {
	HANDLE lock = redirect->connection_lock;
	debug("Free conection %lu  #%p", c->index, c);
	WaitForSingleObject(lock, INFINITE);
	for (UINT32 i = c->index + 1; i < redirect->connection_count; i++) {
		redirect->connections[i]->index--;
		redirect->connections[i - 1] = redirect->connections[i];
	}
	redirect->connection_count--;
	free(c);
	ReleaseMutex(lock);
}

void shutdownAllConnections(PREDIRECT redirect) {
	HANDLE lock = redirect->connection_lock;
	WaitForSingleObject(lock, INFINITE);
	UINT32 connection_count = redirect->connection_count;
	for (UINT32 i = 0; i < connection_count; i++) {
		PTCP_CONNECTION c = redirect->connections[i];
		c->should_free = FALSE;
		c->closed = TRUE;
		WinDivertShutdown(c->handle, WINDIVERT_SHUTDOWN_BOTH);
	}
	for (UINT32 i = 0; i < connection_count; i++) {
		PTCP_CONNECTION c = redirect->connections[i];
		WaitForSingleObject(c->thread_handle, INFINITE);
		if (!WinDivertClose(c->handle)) {
			DWORD err = GetLastError();
			warning("WinDivertClose err (%d), conection %p", err, c);
		}
		c->handle = INVALID_HANDLE_VALUE;
		CloseHandle(c->thread_handle);
	}
	// connection can still be in redirect_in function, 
	// so we just wait for the incoming packet to be processed
	Sleep(50);
	for (UINT32 i = 0; i < connection_count; i++) {
		PTCP_CONNECTION c = redirect->connections[i];
		redirect->connections[i] = 0;
		free(c);
	}
	redirect->connection_count = 0;
	ReleaseMutex(lock);
}
BOOL DstConnectionExists(PREDIRECT redirect, UINT32 DstAddr, UINT16 DstPort) {
	HANDLE lock = redirect->connection_lock;
	WaitForSingleObject(lock, INFINITE);
	UINT32 connection_count = redirect->connection_count;
	for (UINT32 i = 0; i < connection_count; i++) {
		PTCP_CONNECTION c = redirect->connections[i];
		if (c->DstIp == DstAddr && c->DstPort == DstPort && !c->releasing) {
			ReleaseMutex(lock);
			return 1;
		}
	}
	ReleaseMutex(lock);
	return 0;
}

BOOL WinNatConnectionExists(PREDIRECT redirect, PWINDIVERT_IPHDR ip_header, PWINDIVERT_TCPHDR tcp_header) {
	HANDLE lock = redirect->connection_lock;
	WaitForSingleObject(lock, INFINITE);
	UINT32 connection_count = redirect->connection_count;
	for (UINT32 i = 0; i < connection_count; i++) {
		PTCP_CONNECTION c = redirect->connections[i];
		if (c->releasing) {
			continue;
		}
		if (c->win_nat_SrcIp == ip_header->SrcAddr && c->win_nat_SrcPort == tcp_header->SrcPort
			&& c->DstIp == ip_header->DstAddr && c->DstPort == tcp_header->DstPort) {
			ReleaseMutex(lock);
			return 1;
		}
	}
	ReleaseMutex(lock);
	return 0;
}

PTCP_CONNECTION findConnection(PREDIRECT redirect, PWINDIVERT_IPHDR ip_header, PWINDIVERT_TCPHDR tcp_header) {
	HANDLE lock = redirect->connection_lock;
	UINT32 DstAddr = ip_header->DstAddr;
	UINT32 SrcAddr = ip_header->SrcAddr;
	UINT16 DstPort = tcp_header->DstPort;
	UINT16 SrcPort = tcp_header->SrcPort;
	WaitForSingleObject(lock, INFINITE);
	UINT32 connection_count = redirect->connection_count;
	for (UINT32 i = 0; i < connection_count; i++) {
		PTCP_CONNECTION c = redirect->connections[i];
		if (c->releasing) {
			continue;
		}
		if (c->DstIp == DstAddr && c->DstPort == DstPort &&
			c->SrcIp == SrcAddr && c->SrcPort == SrcPort) {
			ReleaseMutex(lock);
			return c;
		}
	}
	ReleaseMutex(lock);
	return 0;
}
DWORD WINAPI cleanup_thread(LPVOID lpParam)
{
	PREDIRECT redirect = (PREDIRECT)lpParam;
	HANDLE forward_handle = redirect->forward_handle;
	debug("Cleaner thread started for %p", forward_handle);
	HANDLE lock = redirect->connection_lock;
	UINT64 lastCheckTime = GetTickCount64();

	while (!redirect->cleanup_thread_interrupted) {
		UINT64 curTime = GetTickCount64();
		if (curTime - lastCheckTime > 5000) {

			WaitForSingleObject(lock, INFINITE);
			for (int i = 0; i < redirect->connection_count; i++) {
				PTCP_CONNECTION c = redirect->connections[i];
				UINT64 diff = curTime - c->LastPacketTime;
				if (c->server_fin || c->client_fin) {
					if (diff > 1 * 60 * 1000) {//1 min
						c->closed = TRUE;
						if (c->handle != 0) {
							info("Close connection 1 min %p", c);
							WinDivertShutdown(c->handle, WINDIVERT_SHUTDOWN_RECV);
						}
						else {
							info("1 min timeout, handle is 0, shouldn't happen!");
						}
					}
				}
				else if (diff > 10 * 60 * 1000) {//10 min
					c->closed = TRUE;
					if (c->handle != 0) {
						info("Close connection 10 min");
						WinDivertShutdown(c->handle, WINDIVERT_SHUTDOWN_RECV);
					}
					else {
						info("10 min timeout, handle is 0, shouldn't happen!");
					}
				}
			}
			ReleaseMutex(lock);
			lastCheckTime = curTime;
		}
		Sleep(200);
	}
	debug("Cleaner thread stopped for %p", forward_handle);
	return 0;
}

//creates next ipv4 address starting from 127.0.0.2
UINT32 next_local_addr(PREDIRECT redirect) {
	if (redirect->next_ip == 0) {
		redirect->next_ip = 2130706433;// 127.0.0.1
	}
	redirect->next_ip++;
	if (redirect->next_ip > 2147483646) { // > 127.255.255.254
		redirect->next_ip = 2130706434; // 127.0.0.2
	}
	return htonl(redirect->next_ip);
}

BOOL is_localhost_address(UINT32 addr) {
	if ((addr & 0xFF) != 127) {
		return FALSE;
	}
	if (addr == 127 || addr == 0xffffff7f) {
		return FALSE;
	}
	return TRUE;
}

BOOL bind_port(UINT16* port_out, SOCKET* socket_out, int protocol) {
	WSADATA wsaData;

	int err = WSAStartup(MAKEWORD(2, 2), &wsaData);
	if (err != 0) {
		error("WSAStartup failed with error: %d", err);
		return FALSE;
	}
	SOCKET sock = socket(AF_INET, protocol == IPPROTO_TCP ? SOCK_STREAM : SOCK_DGRAM, protocol);

	if (sock == INVALID_SOCKET) {
		error("Failed create socket with error: %d", WSAGetLastError());
		return FALSE;
	}
	struct sockaddr_in sin = { 0 };
	sin.sin_family = AF_INET;
	sin.sin_addr.s_addr = inet_addr("0.0.0.0");
	sin.sin_port = 0;

	err = bind(sock, (struct sockaddr*)&sin, sizeof(sin));
	if (err == SOCKET_ERROR) {
		error("Bind failed with error: %d", WSAGetLastError());
		return FALSE;
	}
	socklen_t len = sizeof(sin);
	if (getsockname(sock, (struct sockaddr*)&sin, &len) == -1)
	{
		error("getsockname failed with error: %d", WSAGetLastError());
		return FALSE;
	}
	*port_out = ntohs(sin.sin_port);
	*socket_out = sock;
	return TRUE;
}

BOOL bind_tcp_port(UINT16* port_out, SOCKET* socket_out) {
	return bind_port(port_out, socket_out, IPPROTO_TCP);
}

BOOL bind_udp_port(UINT16* port_out, SOCKET* socket_out) {
	return bind_port(port_out, socket_out, IPPROTO_UDP);
}
BOOL skip_port(PREDIRECT r, UINT16 port) {
	UINT32 count = r->skip_local_ports_count;
	for (UINT32 i = 0; i < count; i++) {
		if (r->skip_local_ports[i] == port) {
			return TRUE;
		}
	}
	return FALSE;
}

DWORD WINAPI redirect_out(LPVOID lpParam)
{
	debug("[S] thread started");

	PTCP_CONNECTION con = (PTCP_CONNECTION)lpParam;

	HANDLE handle = con->handle;

	unsigned char* packet = malloc(MAXBUF);
	if (packet == 0) {
		warning("[S] Failed to allocate packet buffer");
		return 1;
	}
	UINT packet_len;
	WINDIVERT_ADDRESS addr;
	PWINDIVERT_IPHDR ip_header;
	PWINDIVERT_TCPHDR tcp_header;
	PWINDIVERT_UDPHDR udp_header;
	char msg[512];
	while (TRUE)
	{

		if (!WinDivertRecv(handle, packet, MAXBUF, &packet_len, &addr))
		{
			DWORD err = GetLastError();
			if (err == ERROR_NO_DATA) {
				debug("[S] close by shutdown");
				break;
			}

			warning("[S] failed to read packet (%d), conection %p", err, con);
			if (err == 6) {
				break;
			}
			continue;
		}
		con->LastPacketTime = GetTickCount64();
		WinDivertHelperParsePacket(packet, packet_len, &ip_header, NULL, NULL,
			NULL, NULL, &tcp_header, &udp_header, NULL, NULL, NULL, NULL);
		if (ip_header == NULL || (tcp_header == NULL && udp_header == NULL))
		{
			warning("failed to parse packet (%d)", GetLastError());
			continue;
		}
		ip_header->SrcAddr = con->DstIp;// -> 165.22.196.0
		tcp_header->SrcPort = con->DstPort;// -> 80
		ip_header->DstAddr = con->SrcIp;// -> 192.168.137.71
		tcp_header->DstPort = con->SrcPort;// -> port of 192.168.137.71
		if (is_debug()) {
			sprint_tcp_src_dst(ip_header, tcp_header, &addr, msg);
			debug("[S] (%s) %s, ack %lu seq %lu", get_tcp_flag(tcp_header), msg, ntohl(tcp_header->AckNum), ntohl(tcp_header->SeqNum));
		}
		if (tcp_header->Fin) {
			if (con->server_fin) {//simultaneous tcp close
				if (con->server_fin_ack == ntohl(tcp_header->AckNum) && con->server_fin_seq == ntohl(tcp_header->SeqNum)) {
					con->closed = TRUE;
					debug(" [S] RETRANSMISSION? simultaneous tcp close!");
				}
				else {
					con->closed = TRUE;
					debug("[S] simultaneous tcp close!");
				}
			}
			con->server_fin = TRUE;
			con->server_fin_ack = ntohl(tcp_header->AckNum);
			con->server_fin_seq = ntohl(tcp_header->SeqNum);
			debug("[S] server_fin");
		}
		else if (tcp_header->Ack && con->server_fin && !tcp_header->Rst) {
			if (con->server_fin_ack + 1 == ntohl(tcp_header->AckNum) && con->server_fin_seq + 1 == ntohl(tcp_header->SeqNum)) {
				con->closed = TRUE;
				debug("[S] Correct close");
			}
			else {
				debug("[S] NOT fin acknum, seqnum! %lu != %lu or %lu != %lu", con->server_fin_ack + 1, ntohl(tcp_header->AckNum), con->server_fin_seq + 1, ntohl(tcp_header->SeqNum));
			}
		}

		WinDivertHelperCalcChecksums(packet, packet_len, &addr, 0);
		if (!WinDivertSend(handle, packet, packet_len, NULL, &addr))
		{
			warning("[S] failed to send packet (%d)", GetLastError());
			continue;
		}
		if (tcp_header->Rst) {
			con->closed = TRUE;
			debug("[S] RST CLOSE");
		}
		if (con->closed) {
			debug("[S] closed");
			break;
		}

	}
	free(packet);
	debug("[S] Thread end");
	// will be false if shutdown was called by shutdownAllConnections()
	if (!con->should_free) {
		return 0;
	}
	if (!WinDivertShutdown(con->handle, WINDIVERT_SHUTDOWN_BOTH)) {
		DWORD err = GetLastError();
		warning("[S] WinDivertShutdown err (%d), conection %p", err, con);
	}
	if (!WinDivertClose(con->handle)) {
		DWORD err = GetLastError();
		warning("[S] WinDivertClose err (%d), conection %p", err, con);
	}
	con->handle = INVALID_HANDLE_VALUE;
	HANDLE thread_handle = con->thread_handle;
	if (!con->client_fin || !con->server_fin) {
		Sleep(2000);
		con->releasing = TRUE;
		Sleep(750);
	}
	else {
		Sleep(250);
		con->releasing = TRUE;
		Sleep(500);
	}

	freeConnection((PREDIRECT)con->owner, con);
	CloseHandle(thread_handle);
	return 0;
}

DWORD WINAPI redirect_in(LPVOID lpParam)
{
	PREDIRECT redirect = (PREDIRECT)lpParam;

	HANDLE handle = redirect->forward_handle;
	HANDLE net = redirect->network_handle;

	UINT16 redirect_port = redirect->redirect_port;
	UINT16 nredirect_port = htons(redirect_port);

	UINT16 binded_port = redirect->binded_port;
	UINT16 nbinded_port = htons(binded_port);
	unsigned char* packet = redirect->packet;

	char* msg = redirect->buf;
	char layer = redirect->layer;

	UINT packet_len;
	WINDIVERT_ADDRESS addr;
	PWINDIVERT_IPHDR ip_header;
	PWINDIVERT_TCPHDR tcp_header;
	DWORD len;

	UINT32 redirect_ip = ip4("127.0.0.1");

	UINT32 prevAck;
	UINT32 prevSeq;
	PTCP_CONNECTION prev_con;

	debug("Redirect %p started (%s)", redirect, layer == WINDIVERT_LAYER_NETWORK ? "network" : "network forward");

	// Main loop:
	while (TRUE)
	{
		if (!WinDivertRecv(handle, packet, MAXBUF, &packet_len, &addr))
		{
			DWORD err = GetLastError();
			if (err == ERROR_NO_DATA) {
				info("redirect_in closed by shutdown");
				break;
			}
			warning("[C] failed to read packet (%d)", err);
			continue;
		}

		WinDivertHelperParsePacket(packet, packet_len, &ip_header, NULL, NULL,
			NULL, NULL, &tcp_header, NULL, NULL, NULL, NULL, NULL);
		if (ip_header == NULL || tcp_header == NULL)
		{
			warning("[C] failed to parse packet (%d)", GetLastError());
			continue;
		}

		if (layer == WINDIVERT_LAYER_NETWORK_FORWARD && addr.Impostor) {
			// packets that sends by Windows NAT
			// For example 192.168.0.2:59834 -> 1.1.1.1:25565
			// should be sent right after real packet

			if (WinNatConnectionExists(redirect, ip_header, tcp_header)) {
				if (prevAck != tcp_header->AckNum || prevSeq != tcp_header->SeqNum) {
					if (is_info()) {
						sprint_tcp_src_dst(ip_header, tcp_header, &addr, msg);
						info("[C] Skip win nat (%s): %s ack %lu != %lu, seq %lu != %lu", get_tcp_flag(tcp_header), msg, prevAck, tcp_header->AckNum, prevSeq, tcp_header->SeqNum);
					}
				}
				//info("Skip nat ok");
				continue;
			}
			else {
				if (redirect->pause) {
					if (prevAck == tcp_header->AckNum && prevSeq == tcp_header->SeqNum) {
						if (is_warn()) {
							sprint_tcp_src_dst(ip_header, tcp_header, &addr, msg);
							warning("[C] Skip win nat, pause (%s): %s ack %lu != %lu, seq %lu != %lu", get_tcp_flag(tcp_header), msg, prevAck, tcp_header->AckNum, prevSeq, tcp_header->SeqNum);
						}
						info("pause skip ok?");
						continue;
					}
				}
				else if (DstConnectionExists(redirect, ip_header->DstAddr, tcp_header->DstPort)) {
					if (prevAck == tcp_header->AckNum && prevSeq == tcp_header->SeqNum) {
						if (prev_con != 0 && prev_con->win_nat_SrcIp == 0 && prev_con->win_nat_SrcPort == 0) {
							prev_con->win_nat_SrcIp = ip_header->SrcAddr;
							prev_con->win_nat_SrcPort = tcp_header->SrcPort;
							info("init win nat %s %i", ntoa(prev_con->win_nat_SrcIp), ntohs(prev_con->win_nat_SrcPort));
						}
						else {
							info("skip not init nat %s %i", ntoa(prev_con->win_nat_SrcIp), ntohs(prev_con->win_nat_SrcPort));
						}

						continue;
					}
					else {
						warning("!!! [C] Skip impostor (%s): %s ack %lu seq %lu", get_tcp_flag(tcp_header), msg, tcp_header->AckNum, tcp_header->SeqNum);
					}
				}
			}
			//if (!redirect->pause) {
			//	if (DstConnectionExists(redirect, ip_header->DstAddr, tcp_header->DstPort)) {

			//		if (prevAck != tcp_header->AckNum || prevSeq != tcp_header->SeqNum) {
			//			if (is_warn()) {
			//				sprint_tcp_src_dst(ip_header, tcp_header, &addr, msg);
			//				warning("!!! [C] Skip impostor (%s): %s ack %lu seq %lu", get_tcp_flag(tcp_header), msg, tcp_header->AckNum, tcp_header->SeqNum);
			//			}
			//		}
			//		else {
			//			if (prev_con != 0 && prev_con->win_nat_SrcIp == 0 && prev_con->win_nat_SrcPort == 0) {
			//				prev_con->win_nat_SrcIp = ip_header->SrcAddr;
			//				prev_con->win_nat_SrcPort = tcp_header->SrcPort;
			//				info("INIT win nat %s %i", ntoa(prev_con->win_nat_SrcIp), ntohs(prev_con->win_nat_SrcPort));
			//			}
			//		}

			//		sprint_tcp_src_dst(ip_header, tcp_header, &addr, msg);
			//		info("[C] Skip impostor (%s): %s ack %lu seq %lu", get_tcp_flag(tcp_header), msg, tcp_header->AckNum, tcp_header->SeqNum);
			//		continue;
			//	}
			//	else {
			//		if (prevAck == tcp_header->AckNum && prevSeq == tcp_header->SeqNum) {
			//			if (is_warn()) {
			//				sprint_tcp_src_dst(ip_header, tcp_header, &addr, msg);
			//				warning("!!! [C] should skip impostor (%s): %s ack %lu seq %lu", get_tcp_flag(tcp_header), msg, tcp_header->AckNum, tcp_header->SeqNum);
			//			}
			//		}
			//	}
			//}
			//else {
			//	if (prevAck == tcp_header->AckNum && prevSeq == tcp_header->SeqNum) {
			//		sprint_tcp_src_dst(ip_header, tcp_header, &addr, msg);
			//		info("[C] Skip impostor (%s): %s ack %lu seq %lu", get_tcp_flag(tcp_header), msg, tcp_header->AckNum, tcp_header->SeqNum);
			//		continue;
			//	}
			//}
			//sprint_tcp_src_dst(ip_header, tcp_header, &addr, msg);
			debug("[C] Impostor");
		}

		PTCP_CONNECTION con = findConnection(redirect, ip_header, tcp_header);

		if (tcp_header->Syn && !redirect->pause) {
			BOOL canCreate = TRUE;
			if (con != 0) {
				if (con->closed) {
					warning("[C] SYN ? recv for closed tcp");
				}
				else if (con->server_fin || con->client_fin) {
					warning("[C] SYN ? recv for closing tcp");
				}
				else if (con->syn_AckNum == tcp_header->AckNum && con->syn_SeqNum == tcp_header->SeqNum) {
					warning("[C] SYN retransmission");
					canCreate = FALSE;
				}
				else {
					warning("[C] SYN ?");
				}
			}

			if (skip_port(redirect, ntohs(tcp_header->SrcPort))) {
				canCreate = FALSE;
				info("[C] Skip outbound connection from %s:%lu", ntoa(ip_header->SrcAddr), ntohs(tcp_header->SrcPort));
			}
			if (canCreate) {
				UINT32 next_local_ip = next_local_addr(redirect);
				//"tcp and ip.SrcAddr == 192.168.137.1 and tcp.SrcPort == %d and ip.DstAddr == 192.168.137.71 and tcp.DstPort == %d"
				//165.22.196.0:80 -> 192.168.137.71:DstPort
				//localhost:80    -> localhost:binded_port
				snprintf(msg, 2048,
					"tcp and ip.SrcAddr == 127.0.0.1 and tcp.SrcPort == %d and ip.DstAddr == %s and tcp.DstPort == %d",
					redirect_port, ntoa(next_local_ip), binded_port);
				HANDLE out_handle = WinDivertOpen(msg, WINDIVERT_LAYER_NETWORK, REDIRECT_PRIORITY, 0);
				if (out_handle == INVALID_HANDLE_VALUE)
				{
					warning("[C] failed to open the WinDivert device (%d)", GetLastError());
				}
				else {
					PTCP_CONNECTION c = newConnection(redirect);
					if (c != 0) {
						c->SrcIp = ip_header->SrcAddr;// 192.168.137.71
						c->SrcPort = tcp_header->SrcPort;// port of 192.168.137.71
						c->DstIp = ip_header->DstAddr; // 165.22.196.0
						c->DstPort = tcp_header->DstPort; // 80
						c->NewDstIp = redirect_ip;
						c->NewDstPort = nredirect_port;
						c->FakeSrcIp = next_local_ip;

						c->syn_AckNum = tcp_header->AckNum;
						c->syn_SeqNum = tcp_header->SeqNum;
						c->handle = out_handle;

						con = c;
						if (is_debug()) {
							sprint_tcp_src_dst(ip_header, tcp_header, &addr, msg);
							debug("[C] new connection: \"%s\"", msg);
						}

						c->thread_handle = CreateThread(NULL, 100 * 1024, redirect_out, c, 0, 0);
					}
					else {
						WinDivertClose(out_handle);
					}
				}
				//Sleep(10);
			}

		}
		if (con != 0) {
			prevAck = tcp_header->AckNum;
			prevSeq = tcp_header->SeqNum;

			ip_header->TTL++;
			ip_header->SrcAddr = con->FakeSrcIp;
			tcp_header->SrcPort = nbinded_port;
			ip_header->DstAddr = redirect_ip;
			tcp_header->DstPort = nredirect_port;
			addr.Layer = 0;
			addr.Outbound = 1;
			addr.Network.IfIdx = 1;// Software Loopback Interface 1 /127.0.0.1
			if (is_debug()) {
				sprint_tcp_src_dst(ip_header, tcp_header, &addr, msg);
				debug("[C] Send (%s): %s, ack %lu seq %lu", get_tcp_flag(tcp_header), msg, ntohl(tcp_header->AckNum), ntohl(tcp_header->SeqNum));
			}

			//print_iphdr(ip_header);
			//print_tcphdr(tcp_header);
			//print_addr(&addr);
			//print_mem((char*)tcp_header + sizeof(WINDIVERT_TCPHDR), packet + packet_len);
			//printf("");
			if (tcp_header->Fin) {
				if (con->client_fin) {//simultaneous tcp close
					con->closed = TRUE;
					debug("[C] simultaneous tcp close");
				}
				con->client_fin = TRUE;
				con->client_fin_ack = ntohl(tcp_header->AckNum);
				con->client_fin_seq = ntohl(tcp_header->SeqNum);
				debug("[C] client fin");
			}
			else if (tcp_header->Ack && con->client_fin && !tcp_header->Rst) {
				if (con->client_fin_ack + 1 == ntohl(tcp_header->AckNum) && con->client_fin_seq + 1 == ntohl(tcp_header->SeqNum)) {
					con->closed = TRUE;
					debug("[C] Correct close");
				}
				else {
					debug("[C] NOT fin acknum, seqnum! %lu != %lu or %lu != %lu", con->client_fin_ack + 1, ntohl(tcp_header->AckNum), con->client_fin_seq + 1, ntohl(tcp_header->SeqNum));
				}
			}
			con->LastPacketTime = GetTickCount64();

		}
		else {
			if (!redirect->pause) {
				if (is_debug()) {
					sprint_tcp_src_dst(ip_header, tcp_header, &addr, msg);
					debug("[C - NULL] Send (%s): %s, ack %lu seq %lu", get_tcp_flag(tcp_header), msg, ntohl(tcp_header->AckNum), ntohl(tcp_header->SeqNum));
				}

			}
		}

		WinDivertHelperCalcChecksums(packet, packet_len, &addr, 0);
		if (!WinDivertSend(net, packet, packet_len, NULL, &addr))
		{
			warning("[C] failed to send packet (%d)", GetLastError());
			continue;
		}

		if (con != 0) {
			if (tcp_header->Rst) {
				con->closed = TRUE;
				debug("[C] RST CLOSE");
			}
			if (con->closed) {
				WinDivertShutdown(con->handle, WINDIVERT_SHUTDOWN_RECV);
			}
		}
		prev_con = con;
	}
	redirect->cleanup_thread_interrupted = TRUE;
	redirect->forward_handle = 0;
	redirect->network_handle = 0;
	WaitForSingleObject(redirect->thread_clean, INFINITE);
	CloseHandle(redirect->thread_clean);

	shutdownAllConnections(redirect);

	WinDivertShutdown(handle, WINDIVERT_SHUTDOWN_BOTH);
	WinDivertShutdown(net, WINDIVERT_SHUTDOWN_BOTH);

	WinDivertClose(handle);
	WinDivertClose(net);

	if (redirect->connections != 0) {
		free(redirect->connections);
	}
	if (redirect->skip_local_ports != 0) {
		free(redirect->skip_local_ports);
	}
	if (redirect->packet != 0) {
		free(redirect->packet);
	}
	if (redirect->buf != 0) {
		free(redirect->buf);
	}
	if (redirect->connection_lock != 0) {
		CloseHandle(redirect->connection_lock);
	}
	if (redirect->socket != 0) {
		closesocket(redirect->socket);
	}
	CloseHandle(redirect->thread_in);
	free(redirect);
	debug("Redirect %p stopped (%s)", redirect, layer == WINDIVERT_LAYER_NETWORK ? "network" : "network forward");
	return 0;
}

__declspec(dllexport) void redirect_stop(PREDIRECT r) {
	if (r == NULL) {
		error("null");
		return;
	}
	if (r->forward_handle != 0) {
		WinDivertShutdown(r->forward_handle, WINDIVERT_SHUTDOWN_RECV);
	}
}

__declspec(dllexport) BOOL redirect_get_real_addresses(PREDIRECT r, char* ip, UINT16 port, UINT32* RealSrcIp, UINT16* RealSrcPort, UINT32* RealDstIp, UINT16* RealDstPort) {
	if (r == NULL) {
		error("null");
		return FALSE;
	}
	if (ip == NULL) {
		return FALSE;
	}
	if (r->binded_port != port)
	{
		return FALSE;
	}
	UINT32 addr = ip4(ip);
	HANDLE lock = r->connection_lock;

	WaitForSingleObject(lock, INFINITE);
	UINT32 connection_count = r->connection_count;
	for (UINT32 i = 0; i < connection_count; i++) {
		PTCP_CONNECTION c = r->connections[i];
		if (c->releasing) {
			continue;
		}
		if (c->FakeSrcIp == addr) {
			*RealSrcIp = c->SrcIp;
			*RealSrcPort = c->SrcPort;
			*RealDstIp = c->DstIp;
			*RealDstPort = c->DstPort;
			ReleaseMutex(lock);
			return TRUE;
		}
	}
	ReleaseMutex(lock);
	return FALSE;
}

__declspec(dllexport) UINT32 redirect_get_active_connections_count(PREDIRECT r) {
	if (r == NULL) {
		error("null");
		return 0;
	}
	return r->connection_count;
}

__declspec(dllexport) BOOL redirect_remove_skip_port(PREDIRECT r, UINT16 port) {
	if (r == NULL) {
		error("null");
		return FALSE;
	}
	UINT32 count = r->skip_local_ports_count;
	for (UINT32 i = 0; i < count; i++) {
		if (r->skip_local_ports[i] == port) {
			for (UINT32 j = i; j < count - 1; j++) {
				r->skip_local_ports[j] = r->skip_local_ports[j + 1];
			}
			//memmove(r->skip_local_ports + i, r->skip_local_ports + i + 1, (count - i - 1) * sizeof(UINT16));
			r->skip_local_ports_count--;
			return TRUE;
		}
	}
	return FALSE;
}

__declspec(dllexport) BOOL redirect_add_skip_port(PREDIRECT r, UINT16 port) {
	if (r == NULL) {
		error("null");
		return FALSE;
	}
	UINT32 index = r->skip_local_ports_count;

	if (index >= r->skip_local_ports_size) {
		UINT32 new_size = r->skip_local_ports_size / 2 * 3;
		UINT16* new_ports = realloc(r->skip_local_ports, new_size * sizeof(UINT16));
		if (new_ports == 0) {
			error("Failed to reallocate new local ports %d -> %d", r->skip_local_ports_size, new_size);
			return 0;
		}
		r->skip_local_ports_size = new_size;
		r->skip_local_ports = new_ports;
	}
	r->skip_local_ports[index] = port;
	r->skip_local_ports_count = index + 1;
	return 1;
}

__declspec(dllexport) PREDIRECT redirect_start(UINT16 port, char* filter, char layer) {

	if (layer != WINDIVERT_LAYER_NETWORK && layer != WINDIVERT_LAYER_NETWORK_FORWARD) {
		error("Unknown layer %i", layer);
		return 0;
	}
	if (filter == NULL) {
		error("Filter is null");
		return 0;
	}
	PREDIRECT r = calloc(1, sizeof(REDIRECT));
	if (r == 0) {
		error("Failed to allocate");
		return 0;
	}
	r->layer = layer;
	r->connection_count = 0;
	r->connections_size = 256;
	r->connections = calloc(r->connections_size, sizeof(PTCP_CONNECTION));
	if (r->connections == 0) {
		error("Failed to allocate buffer for connections");
		goto err;
	}

	r->skip_local_ports_count = 0;
	r->skip_local_ports_size = 32;
	r->skip_local_ports = calloc(r->skip_local_ports_size, sizeof(UINT16));
	if (r->skip_local_ports == 0) {
		error("Failed to allocate buffer for skip ports");
		goto err;
	}

	r->packet = malloc(MAXBUF);
	if (r->packet == 0) {
		error("Failed to allocate packet buffer");
		goto err;
	}
	r->buf = malloc(2048);
	if (r->buf == 0) {
		error("Failed to allocate buffer");
		goto err;
	}

	r->connection_lock = CreateMutex(NULL, FALSE, NULL);
	if (r->connection_lock == 0) {
		error("Failed to create mutex (%d)", GetLastError());
		goto err;
	}
	r->cleanup_thread_interrupted = FALSE;
	r->redirect_port = port;

	if (!bind_tcp_port(&(r->binded_port), &(r->socket))) {
		goto err;
	}

	r->network_handle = WinDivertOpen("false", WINDIVERT_LAYER_NETWORK, REDIRECT_PRIORITY, WINDIVERT_FLAG_DROP);
	if (r->network_handle == INVALID_HANDLE_VALUE)
	{
		error("Failed to open the WinDivert device (network) (%d)", GetLastError());
		goto err;
	}
	r->forward_handle = WinDivertOpen(filter, layer, REDIRECT_PRIORITY, 0);
	if (r->forward_handle == INVALID_HANDLE_VALUE)
	{
		error("Failed to open the WinDivert device (forward) (%d)", GetLastError());
		goto err;
	}

	r->thread_clean = CreateThread(NULL, 100 * 1024, cleanup_thread, r, 0, 0);
	r->thread_in = CreateThread(NULL, 256 * 1024, redirect_in, r, 0, 0);
	if (r->thread_in == 0) {
		error("Failed to create thread (%d)", GetLastError());
		goto err;
	}
	return r;

err:

	if (r->connections != 0) {
		free(r->connections);
	}
	if (r->skip_local_ports != 0) {
		free(r->skip_local_ports);
	}
	if (r->packet != 0) {
		free(r->packet);
	}
	if (r->buf != 0) {
		free(r->buf);
	}
	if (r->connection_lock != 0) {
		CloseHandle(r->connection_lock);
	}
	if (r->socket != 0) {
		closesocket(r->socket);
	}
	if (r->network_handle != INVALID_HANDLE_VALUE && r->network_handle != 0)
	{
		WinDivertClose(r->network_handle);
	}
	if (r->forward_handle != INVALID_HANDLE_VALUE && r->forward_handle != 0)
	{
		WinDivertClose(r->forward_handle);
	}

	if (r->thread_clean != 0) {
		r->cleanup_thread_interrupted = TRUE;
		WaitForSingleObject(r->thread_clean, INFINITE);
		CloseHandle(r->thread_clean);

	}
	if (r->thread_in != 0) {
		CloseHandle(r->thread_in);
	}
	free(r);
	return 0;
}
__declspec(dllexport) void redirect_pause(PREDIRECT r) {
	if (r == NULL) {
		error("null");
		return;
	}
	((PREDIRECT)r)->pause = TRUE;
}

__declspec(dllexport) void redirect_resume(PREDIRECT r) {
	if (r == NULL) {
		error("null");
		return;
	}
	((PREDIRECT)r)->pause = FALSE;
}

DWORD WINAPI disable_mdns_thread(LPVOID lpParam)
{
	PMDNS mdns = (PMDNS)lpParam;
	HANDLE handle = mdns->windivert_handle;
	unsigned char* packet = mdns->packet;
	UINT packet_len;
	WINDIVERT_ADDRESS addr;
	PWINDIVERT_IPHDR ip_header = 0;
	PWINDIVERT_UDPHDR udp_header = 0;
	DWORD len;
	info("Started mdns");
	while (TRUE)
	{
		if (!WinDivertRecv(handle, packet, MAXBUF, &packet_len, &addr))
		{
			DWORD err = GetLastError();
			if (err == ERROR_NO_DATA) {
				//info("[C] close by shutdown");
				break;
			}
			warning("[C] failed to read packet (%d)", err);
			continue;
		}

		//WinDivertHelperParsePacket(packet, packet_len, &ip_header, NULL, NULL,
		//	NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
		//if (ip_header == NULL)
		//{
		//	warning("[C] failed to parse packet (%d)", GetLastError());
		//	continue;
		//}
		//WinDivertHelperCalcChecksums(packet, packet_len, &addr, 0);
		//if (!WinDivertSend(handle, packet, packet_len, NULL, &addr))
		//{
		//	warning("[C] failed to send packet (%d)", GetLastError());
		//	continue;
		//}
	}

	WinDivertShutdown(handle, WINDIVERT_SHUTDOWN_BOTH);
	WinDivertClose(handle);
	info("Stopped mdns");
	return 0;
}

__declspec(dllexport) PMDNS mdns_llmnr_disable(char* local_ip) {//etc 192.168.137.1

	PMDNS mdns = calloc(1, sizeof(MDNS));
	if (mdns == NULL) {
		error("Failed to allocate");
		return 0;
	}

	if (local_ip == NULL) {
		mdns->windivert_handle = WinDivertOpen("udp.SrcPort == 5353 or udp.SrcPort == 5355", WINDIVERT_LAYER_NETWORK, MDNS_PRIORITY, 0);
	}
	else {
		char buf[128];
		sprintf(buf, "ip.SrcAddr == %s and (udp.SrcPort == 5353 or udp.SrcPort == 5355)", local_ip);
		mdns->windivert_handle = WinDivertOpen(buf, WINDIVERT_LAYER_NETWORK, MDNS_PRIORITY, 0);
	}

	if (mdns->windivert_handle == INVALID_HANDLE_VALUE)
	{
		error("Failed to open the WinDivert device (forward) (%d)", GetLastError());
		goto err;
	}

	mdns->packet = malloc(MAXBUF);
	if (mdns->packet == 0) {
		error("Failed to allocate packet buffer");
		goto err;
	}

	mdns->thread = CreateThread(NULL, 256 * 1024, disable_mdns_thread, mdns, 0, 0);
	if (mdns->thread == 0) {
		error("Failed to create thread (%d)", GetLastError());
		goto err;
	}
	return mdns;
err:

	if (mdns->windivert_handle != INVALID_HANDLE_VALUE && mdns->windivert_handle != 0)
	{
		WinDivertClose(mdns->windivert_handle);
	}
	if (mdns->packet != 0) {
		free(mdns->packet);
	}
	if (mdns->thread != 0) {
		CloseHandle(mdns->thread);
	}
	free(mdns);
	return 0;
}

__declspec(dllexport) BOOL mdns_restore(PMDNS mdns) {
	if (mdns == 0) {
		error("null");
		return FALSE;
	}
	if (mdns->windivert_handle != 0) {
		WinDivertShutdown(mdns->windivert_handle, WINDIVERT_SHUTDOWN_RECV);
	}
	if (mdns->thread != 0) {
		WaitForSingleObject(mdns->thread, INFINITE);
		CloseHandle(mdns->thread);
	}
	free(mdns->packet);
	free(mdns);
	mdns = 0;
	return TRUE;
}

DWORD WINAPI ttl(LPVOID lpParam)
{
	info("Started ttl");
	PTTL_FIX ttlfix = (PTTL_FIX)lpParam;
	HANDLE handle = ttlfix->forward_handle;
	HANDLE net = ttlfix->network_handle;
	unsigned char* packet = ttlfix->packet;
	UINT packet_len;
	WINDIVERT_ADDRESS addr;
	PWINDIVERT_IPHDR ip_header = 0;
	PWINDIVERT_TCPHDR tcp_header = 0;
	PWINDIVERT_UDPHDR udp_header = 0;
	PWINDIVERT_ICMPHDR icmp_header = 0;
	DWORD len;

	while (TRUE)
	{
		if (!WinDivertRecv(handle, packet, MAXBUF, &packet_len, &addr))
		{
			DWORD err = GetLastError();
			if (err == ERROR_NO_DATA) {
				//info("[C] close by shutdown");
				break;
			}
			warning("[C] failed to read packet (%d)", err);
			continue;
		}

		WinDivertHelperParsePacket(packet, packet_len, &ip_header, NULL, NULL,
			NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
		if (ip_header == NULL)
		{
			warning("[C] failed to parse packet (%d)", GetLastError());
			continue;
		}
		ip_header->TTL++;
		if (ip_header->TTL > 1 && ip_header->TTL < 18) {
			ip_header->TTL += 1;
		}
		//sprint_packet(ip_header, &addr, msg);
		//info("[C] Send: %s, ttl %lu", msg, ip_header->TTL);
		//print_iphdr(ip_header);
		//print_icmphdr(icmp_header);
		//print_addr(&addr);
		//info("");
		WinDivertHelperCalcChecksums(packet, packet_len, &addr, 0);
		if (!WinDivertSend(net, packet, packet_len, NULL, &addr))
		{
			warning("[C] failed to send packet (%d)", GetLastError());
			continue;
		}
	}
	WinDivertShutdown(net, WINDIVERT_SHUTDOWN_BOTH);
	WinDivertShutdown(handle, WINDIVERT_SHUTDOWN_BOTH);
	WinDivertClose(net);
	WinDivertClose(handle);
	info("Stopped ttl");
	return 0;
}
PTTL_FIX ttl_fix;
__declspec(dllexport) BOOL enable_ttl_fix() {
	if (ttl_fix != 0) {
		error("Already enabled");
		return FALSE;
	}
	ttl_fix = calloc(1, sizeof(TTL_FIX));
	if (ttl_fix == 0) {
		error("Failed to allocate");
		return FALSE;
	}

	ttl_fix->network_handle = WinDivertOpen("false", WINDIVERT_LAYER_NETWORK, TTL_PRIORITY, WINDIVERT_FLAG_DROP);
	if (ttl_fix->network_handle == INVALID_HANDLE_VALUE)
	{
		error("Failed to open the WinDivert device (network) (%d)", GetLastError());
		goto err;
	}

	ttl_fix->forward_handle = WinDivertOpen("true", WINDIVERT_LAYER_NETWORK_FORWARD, TTL_PRIORITY, 0);
	if (ttl_fix->forward_handle == INVALID_HANDLE_VALUE)
	{
		error("Failed to open the WinDivert device (forward) (%d)", GetLastError());
		goto err;
	}

	ttl_fix->packet = malloc(MAXBUF);
	if (ttl_fix->packet == 0) {
		error("Failed to allocate packet buffer");
		goto err;
	}

	ttl_fix->thread = CreateThread(NULL, 256 * 1024, ttl, ttl_fix, 0, 0);
	if (ttl_fix->thread == 0) {
		error("Failed to create thread (%d)", GetLastError());
		goto err;
	}
	return TRUE;
err:

	if (ttl_fix->network_handle != INVALID_HANDLE_VALUE && ttl_fix->network_handle != 0)
	{
		WinDivertClose(ttl_fix->network_handle);
	}
	if (ttl_fix->forward_handle != INVALID_HANDLE_VALUE && ttl_fix->forward_handle != 0)
	{
		WinDivertClose(ttl_fix->forward_handle);
	}
	if (ttl_fix->packet != 0) {
		free(ttl_fix->packet);
	}
	if (ttl_fix->thread != 0) {
		CloseHandle(ttl_fix->thread);
	}
	free(ttl_fix);
	return FALSE;
}

__declspec(dllexport) BOOL disable_ttl_fix() {
	if (ttl_fix == 0) {
		error("null");
		return FALSE;
	}
	if (ttl_fix->forward_handle != 0) {
		WinDivertShutdown(ttl_fix->forward_handle, WINDIVERT_SHUTDOWN_RECV);
	}
	if (ttl_fix->thread != 0) {
		WaitForSingleObject(ttl_fix->thread, INFINITE);
		CloseHandle(ttl_fix->thread);
	}
	free(ttl_fix->packet);
	free(ttl_fix);
	ttl_fix = 0;
	return TRUE;
}



PPORT_FORWARD newForwardConnection(PFORWARD f) {
	PPORT_FORWARD connection = calloc(1, sizeof(PORT_FORWARD));
	if (connection == 0) {
		error("Failed to allocate new connection");
		return 0;
	}
	connection->owner = f;
	connection->LastPacketTime = GetTickCount64();

	UINT32 index = f->connection_count;

	if (index >= f->connections_size) {
		UINT32 newSize = f->connections_size / 2 * 3;
		PPORT_FORWARD* newConnections = realloc(f->connections, newSize * sizeof(PORT_FORWARD));
		if (newConnections == 0) {
			error("Failed to reallocate new connection %d -> %d", f->connections_size, newSize);
			return 0;
		}
		f->connections_size = newSize;
		f->connections = newConnections;
	}
	f->connections[index] = connection;
	connection->index = index;
	f->connection_count = index + 1;
	return connection;
}

void removeForwardConnection(PFORWARD f, PPORT_FORWARD c) {
	for (UINT32 i = c->index + 1; i < f->connection_count; i++) {
		f->connections[i]->index--;
		f->connections[i - 1] = f->connections[i];
	}
	if (c->socket != 0) {
		closesocket(c->socket);
		if (c->protocol == IPPROTO_TCP) {
			f->tcp_port_count--;
		}
		else {
			f->udp_port_count--;
		}
	}
	f->connection_count--;
	free(c);
}

void removeFirstUdpForwardConnection(PFORWARD f) {
	PPORT_FORWARD c = NULL;
	for (UINT32 i = 0; i < f->connection_count; i++) {
		if (f->connections[i]->protocol == IPPROTO_UDP) {
			c = f->connections[i];
			break;
		}
	}
	if (c == NULL) {
		return;
	}
	for (UINT32 i = c->index + 1; i < f->connection_count; i++) {
		f->connections[i]->index--;
		f->connections[i - 1] = f->connections[i];
	}
	if (c->socket != 0) {
		closesocket(c->socket);
		if (c->protocol == IPPROTO_TCP) {
			f->tcp_port_count--;
		}
		else {
			f->udp_port_count--;
		}
	}
	f->connection_count--;
	free(c);
}

PPORT_FORWARD getPortForwardBySrc(PFORWARD f, UINT32 ip, UINT16 port, char protocol) {
	for (UINT32 i = 0; i < f->connection_count; i++) {
		PPORT_FORWARD con = f->connections[i];
		if (con->SrcIp == ip && con->SrcPort == port && con->protocol == protocol) {
			return con;
		}
	}
	return NULL;
}

PPORT_FORWARD getPortForwardByPort(PFORWARD f, UINT16 port, char protocol) {
	for (UINT32 i = 0; i < f->connection_count; i++) {
		PPORT_FORWARD con = f->connections[i];
		if (con->local_port == port && con->protocol == protocol) {
			return con;
		}
	}
	return NULL;
}
void port_forward_clean(PFORWARD f) {
	UINT64 cur_time = GetTickCount64();
	for (INT32 i = f->connection_count - 1; i >= 0; i--) {
		PPORT_FORWARD con = f->connections[i];
		UINT64 diff = cur_time - con->LastPacketTime;
		if (con->protocol == IPPROTO_TCP) {
			if (con->server_fin || con->client_fin) {
				if (diff > 1 * 60 * 1000) {//1 min
					con->closed = TRUE;
					debug("Close tcp after 1 min (fin)");
				}
			}
			if (con->syn) {
				if (diff > 10 * 1000) {//10 sec
					con->closed = TRUE;
					debug("Close tcp after 10 sec (syn)");
				}
			}
			if (diff > 10 * 60 * 1000) {//10 min
				con->closed = TRUE;
				debug("Close tcp after 10 min");
			}
		}
		else {
			if (con->DstPort == 53) {
				if (diff > 6 * 1000) {//6 sec timeout for dns 
					con->closed = TRUE;
					debug("Close udp by 8 sec");
				}
			}
			else if (diff > 30 * 1000) {//30 sec udp timeout
				con->closed = TRUE;
				debug("Close udp by 30 sec");
			}
		}
		if (con->closed) {
			removeForwardConnection(f, con);
		}
	}

}

BOOL isSkipPortForward(PFORWARD forward, UINT16 port) {
	if (forward->skip_ports == 0) {
		return FALSE;
	}
	for (INT32 i = 0; i < forward->skip_ports_count; i++) {
		if (forward->skip_ports[i] == port)
		{
			return TRUE;
		}
	}
	return FALSE;
}

DWORD WINAPI port_forward_thread(LPVOID lpParam)
{
	info("Started port forward");
	PFORWARD forward = (PFORWARD)lpParam;

	unsigned char* packet = forward->packet;

	char* msg = forward->buf;

	UINT packet_len;
	WINDIVERT_ADDRESS addr;
	PWINDIVERT_IPHDR ip_header = 0;
	//PWINDIVERT_IPV6HDR ipv6_header = 0;
	PWINDIVERT_TCPHDR tcp_header = 0;
	PWINDIVERT_UDPHDR udp_header = 0;
	DWORD len;

	UINT32 forward_to_addr = forward->forward_to_addr;
	UINT32 this_machine_addr = forward->this_machine_addr;
	UINT32 listen_addr = forward->listen_addr;

	HANDLE handle = forward->windivert;

	while (TRUE)
	{
		ip_header = 0;
		tcp_header = 0;
		udp_header = 0;
		if (!WinDivertRecv(handle, packet, MAXBUF, &packet_len, &addr))
		{
			DWORD err = GetLastError();

			if (err == ERROR_NO_DATA) {
				info("port_forward close by shutdown");
				break;
			}
			warning("[C] failed to read packet (%d)", err);
			continue;
		}

		WinDivertHelperParsePacket(packet, packet_len, &ip_header, NULL, NULL,
			NULL, NULL, &tcp_header, &udp_header, NULL, NULL, NULL, NULL);
		if (ip_header == NULL)
		{
			warning("[C] failed to parse packet (%d)", GetLastError());
			continue;
		}
		//ip_header->TTL++;
		//if (ip_header->TTL > 1 && ip_header->TTL < 18) {
		//	ip_header->TTL += 1;
		//}

		UINT64 cur_time = GetTickCount64();
		//info("[C] Send: %s, ttl %lu", msg, ip_header->TTL);
		PPORT_FORWARD c = 0;
		if (tcp_header != 0) {
			if (isSkipPortForward(forward, ntohs(tcp_header->DstPort))) {
				goto send_packet;
			}
			if (is_debug()) {
				sprint_tcp_src_dst(ip_header, tcp_header, &addr, msg);
				debug("[C] Send: %s %s %i", msg, get_tcp_flag(tcp_header), addr.Outbound);
			}

			if (ip_header->DstAddr == listen_addr) {//192.168.137.1
				//client to server

				c = getPortForwardBySrc(forward, ip_header->SrcAddr, tcp_header->SrcPort, IPPROTO_TCP);
				if (tcp_header->Syn && !tcp_header->Ack) {

					BOOL canCreate = TRUE;
					if (c != 0) {
						if (c->server_fin || c->client_fin) {
							debug("[C] SYN ? recv for closing tcp");
						}
						else if (c->syn_AckNum == tcp_header->AckNum && c->syn_SeqNum == tcp_header->SeqNum) {
							debug("[C] SYN retransmission");
							canCreate = FALSE;
						}
						else {
							debug("[C] SYN ?");
						}
					}
					UINT16 port;
					SOCKET socket;
					if (canCreate) {
						if (forward->tcp_port_count > 1000) {
							warning("Skip new connection, reason > 1000");
							//too many connections. Seems like tcp spam, skip this
							continue;
						}
						if (bind_tcp_port(&port, &socket)) {
							forward->tcp_port_count++;
						}
						else {
							warning("Failed to bind tcp port %s", get_error());
							canCreate = FALSE;
						}
					}
					if (canCreate) {

						c = newForwardConnection(forward);
						if (c == 0) {
							warning("Failed to create tcp forward %s", get_error());
							goto send_packet;
						}
						debug("new forward connection %i", forward->connection_count);
						c->syn = TRUE;
						c->DstPort = ntohs(tcp_header->DstPort);
						c->protocol = IPPROTO_TCP;
						c->SrcIp = ip_header->SrcAddr;
						c->SrcPort = tcp_header->SrcPort;

						c->syn_AckNum = tcp_header->AckNum;
						c->syn_SeqNum = tcp_header->SeqNum;
						c->local_port = port;
						c->socket = socket;
					}
				}
				if (c != 0) {
					if (c->syn && tcp_header->Ack && !tcp_header->Syn) {
						c->syn = FALSE;
					}
					ip_header->SrcAddr = this_machine_addr;//192.168.0.120
					ip_header->DstAddr = forward_to_addr;//192.168.0.1
					tcp_header->SrcPort = c->local_port;

					if (tcp_header->Rst) {
						c->closed = TRUE;
					}
					else if (tcp_header->Fin) {
						if (c->client_fin) {//simultaneous tcp close
							c->closed = TRUE;
							debug("[C] simultaneous tcp close");
						}
						c->client_fin = TRUE;
						c->client_fin_ack = ntohl(tcp_header->AckNum);
						c->client_fin_seq = ntohl(tcp_header->SeqNum);
						debug("[C] client fin");
					}
					else if (tcp_header->Ack && c->client_fin) {
						if (c->client_fin_ack + 1 == ntohl(tcp_header->AckNum) && c->client_fin_seq + 1 == ntohl(tcp_header->SeqNum)) {
							c->closed = TRUE;
							debug("[C] Correct close");
						}
						else {
							debug("[C] NOT fin acknum, seqnum! %lu != %lu or %lu != %lu", c->client_fin_ack + 1, ntohl(tcp_header->AckNum), c->client_fin_seq + 1, ntohl(tcp_header->SeqNum));
						}
					}
					c->LastPacketTime = cur_time;
				}
			}
			else if (ip_header->SrcAddr == forward_to_addr) {
				c = getPortForwardByPort(forward, tcp_header->DstPort, IPPROTO_TCP);
				if (c != 0) {
					if (tcp_header->Rst) {
						c->closed = TRUE;
					}
					else if (tcp_header->Fin) {
						if (c->server_fin) {//simultaneous tcp close
							if (c->server_fin_ack == ntohl(tcp_header->AckNum) && c->server_fin_seq == ntohl(tcp_header->SeqNum)) {
								c->closed = TRUE;
								debug(" [S] RETRANSMISSION? simultaneous tcp close!");
							}
							else {
								c->closed = TRUE;
								debug("[S] simultaneous tcp close!");
							}
						}
						c->server_fin = TRUE;
						c->server_fin_ack = ntohl(tcp_header->AckNum);
						c->server_fin_seq = ntohl(tcp_header->SeqNum);
						debug("[S] server_fin");
					}
					else if (tcp_header->Ack && c->server_fin) {
						if (c->server_fin_ack + 1 == ntohl(tcp_header->AckNum) && c->server_fin_seq + 1 == ntohl(tcp_header->SeqNum)) {
							c->closed = TRUE;
							debug("[S] Correct close");
						}
						else {
							debug("[S] NOT fin acknum, seqnum! %lu != %lu or %lu != %lu", c->server_fin_ack + 1, ntohl(tcp_header->AckNum), c->server_fin_seq + 1, ntohl(tcp_header->SeqNum));
						}
					}

					ip_header->SrcAddr = listen_addr;
					ip_header->DstAddr = c->SrcIp;
					tcp_header->DstPort = c->SrcPort;
					c->LastPacketTime = cur_time;
				}

			}

		}
		else if (udp_header != 0) {
			if (isSkipPortForward(forward, ntohs(udp_header->DstPort))) {
				goto send_packet;
			}
			if (is_debug()) {
				sprint_udp_src_dst(ip_header, udp_header, &addr, msg);
				debug("[C] Send: %s %i", msg, addr.Outbound);
			}
			if (ip_header->DstAddr == listen_addr) {
				c = getPortForwardBySrc(forward, ip_header->SrcAddr, udp_header->SrcPort, IPPROTO_UDP);
				if (c == NULL) {
					if (forward->udp_port_count > 2500) {
						warning("Remove first udp connection, reason > 2500");
						removeFirstUdpForwardConnection(forward);
					}
					UINT16 port;
					SOCKET socket;
					if (bind_udp_port(&port, &socket)) {
						forward->udp_port_count++;
						c = newForwardConnection(forward);
						if (c == 0) {
							warning("Failed to create udp forward %s", get_error());
							goto send_packet;
						}
						debug("new forward udp connection %i, dst %i", forward->connection_count, udp_header->DstPort);
						c->DstPort = ntohs(udp_header->DstPort);
						c->protocol = IPPROTO_UDP;
						c->SrcIp = ip_header->SrcAddr;
						c->SrcPort = udp_header->SrcPort;

						c->local_port = port;
						c->socket = socket;
					}
					else {
						warning("Failed to bind udp port %s", get_error());
					}
				}
				if (c != NULL) {
					ip_header->SrcAddr = this_machine_addr;
					ip_header->DstAddr = forward_to_addr;
					udp_header->SrcPort = c->local_port;
				}
			}
			else if (ip_header->SrcAddr == forward_to_addr) {
				c = getPortForwardByPort(forward, udp_header->DstPort, IPPROTO_UDP);
				if (c != 0) {
					ip_header->SrcAddr = listen_addr;
					ip_header->DstAddr = c->SrcIp;
					udp_header->DstPort = c->SrcPort;
					c->LastPacketTime = cur_time;
					if (c->DstPort == 53) {
						c->closed = TRUE;
					}
				}
			}
		}
		if (c != 0 && c->closed) {
			removeForwardConnection(forward, c);
			debug("remove connection %p %i", c, forward->connection_count);
		}
		//print_iphdr(ip_header);
		//print_icmphdr(icmp_header);
		//print_addr(&addr);
		//info("");

	send_packet:
		WinDivertHelperCalcChecksums(packet, packet_len, &addr, 0);
		if (!WinDivertSend(handle, packet, packet_len, NULL, &addr))
		{
			warning("[C] failed to send packet (%d)", GetLastError());
			continue;
		}

		if (cur_time - forward->last_clean_time > 1500) {
			port_forward_clean(forward);
			forward->last_clean_time = cur_time;
		}
	}

	WinDivertShutdown(handle, WINDIVERT_SHUTDOWN_BOTH);
	if (forward->connections != 0) {
		free(forward->connections);
	}
	if (forward->skip_ports != 0) {
		free(forward->skip_ports);
	}
	if (forward->packet != 0) {
		free(forward->packet);
	}
	if (forward->buf != 0) {
		free(forward->buf);
	}
	if (forward->windivert != INVALID_HANDLE_VALUE && forward->windivert != 0)
	{
		WinDivertClose(forward->windivert);
	}

	if (forward->thread_in != 0) {
		CloseHandle(forward->thread_in);
	}
	free(forward);
	info("Stopped port forward");
	return 0;
}
__declspec(dllexport) BOOL port_forward_stop(PFORWARD forward) {
	if (forward == NULL) {
		error("null");
		return FALSE;
	}
	if (forward->windivert != 0) {
		WinDivertShutdown(forward->windivert, WINDIVERT_SHUTDOWN_RECV);

		return TRUE;
	}
	error("null windivert");
	return FALSE;
}
__declspec(dllexport) PFORWARD port_forward_start(char* listen_ip, char* listen_start_ip, char* listen_end_ip, char* to_ip, char* this_ip, UINT16* skip_ports, UINT16 skip_ports_count) {
	if (listen_ip == 0 || listen_start_ip == 0 || listen_end_ip == 0 || to_ip == 0 || this_ip == 0) {
		error("null parameter");
		return 0;
	}
	PFORWARD forward = calloc(1, sizeof(FORWARD));
	if (forward == 0) {
		error("Failed to allocate");
		return 0;
	}

	forward->connections_size = 128;
	forward->connections = calloc(forward->connections_size, sizeof(PORT_FORWARD));
	if (forward->connections == 0) {
		error("Failed to allocate buffer for connections");
		goto err;
	}

	forward->packet = malloc(MAXBUF);
	if (forward->packet == 0) {
		error("Failed to allocate packet buffer");
		goto err;
	}
	forward->buf = malloc(2048);
	if (forward->buf == 0) {
		error("Failed to allocate buffer");
		goto err;
	}
	if (skip_ports != NULL) {

		forward->skip_ports = malloc(skip_ports_count * sizeof(UINT16));
		if (forward->skip_ports == 0) {
			error("Failed to allocate buffer for skip ports");
			goto err;
		}
		forward->skip_ports_count = skip_ports_count;
		memcpy(forward->skip_ports, skip_ports, skip_ports_count * sizeof(UINT16));
	}

	forward->forward_to_addr = ip4(to_ip);
	forward->this_machine_addr = ip4(this_ip);
	forward->listen_addr = ip4(listen_ip);
	forward->listen_start_addr = ip4(listen_start_ip);
	forward->listen_end_addr = ip4(listen_end_ip);
	char* filter = malloc(2048);
	if (filter == NULL) {
		error("Failed to allocate filter");
		goto err;
	}
	sprintf(filter, "(ip.DstAddr == %s and ip.SrcAddr >= %s and ip.SrcAddr <= %s or ip.SrcAddr == %s and ip.DstAddr == %s) and (tcp or udp)", listen_ip, listen_start_ip, listen_end_ip, to_ip, this_ip);
	forward->windivert = WinDivertOpen(filter, WINDIVERT_LAYER_NETWORK, PORT_FORWARD_PRIORITY, 0);
	free(filter);
	if (forward->windivert == INVALID_HANDLE_VALUE)
	{
		error("Failed to open the WinDivert device (%d)", GetLastError());
		goto err;
	}

	forward->thread_in = CreateThread(NULL, 256 * 1024, port_forward_thread, forward, 0, 0);
	if (forward->thread_in == 0) {
		error("Failed to create thread (%d)", GetLastError());
		goto err;
	}
	return forward;

err:

	if (forward->connections != 0) {
		free(forward->connections);
	}
	if (forward->skip_ports != 0) {
		free(forward->skip_ports);
	}
	if (forward->packet != 0) {
		free(forward->packet);
	}
	if (forward->buf != 0) {
		free(forward->buf);
	}
	if (forward->windivert != INVALID_HANDLE_VALUE && forward->windivert != 0)
	{
		WinDivertClose(forward->windivert);
	}

	if (forward->thread_in != 0) {
		CloseHandle(forward->thread_in);
	}
	free(forward);
	return 0;
}


DWORD WINAPI block_ip_thread(LPVOID lpParam)
{

	PBLOCK_IP block = (PBLOCK_IP)lpParam;
	HANDLE handle = block->windivert;

	unsigned char* packet = block->packet;
	UINT packet_len;
	WINDIVERT_ADDRESS addr;
	PWINDIVERT_IPHDR ip_header = 0;
	PWINDIVERT_TCPHDR tcp_header = 0;
	info("Started ip block");
	while (TRUE)
	{
		if (!WinDivertRecv(handle, packet, MAXBUF, &packet_len, &addr))
		{
			DWORD err = GetLastError();
			if (err == ERROR_NO_DATA) {
				//info("[C] close by shutdown");
				break;
			}
			warning("[C] failed to read packet (%d)", err);
			continue;
		}

		//WinDivertHelperParsePacket(packet, packet_len, &ip_header, NULL, NULL,
		//	NULL, NULL, &tcp_header, NULL, NULL, NULL, NULL, NULL);
		//if (tcp_header != 0) {
		//	print_packet_src_dst(ip_header, tcp_header, &addr);
		//}
	}

	WinDivertShutdown(handle, WINDIVERT_SHUTDOWN_BOTH);
	WinDivertClose(handle);
	free(block->packet);
	CloseHandle(block->thread_in);
	free(block);
	info("Stopped ip block");
	return 0;
}
__declspec(dllexport) BOOL block_ip_stop(PBLOCK_IP block) {
	if (block == NULL)
	{
		error("null");
		return FALSE;
	}
	WinDivertShutdown(block->windivert, WINDIVERT_SHUTDOWN_RECV);
	return TRUE;
}
__declspec(dllexport) PBLOCK_IP block_ip_start(char* filter, char layer) {
	if (layer != WINDIVERT_LAYER_NETWORK && layer != WINDIVERT_LAYER_NETWORK_FORWARD) {
		error("Unknown layer %i", layer);
		return 0;
	}
	PBLOCK_IP block = calloc(1, sizeof(BLOCK_IP));
	if (block == 0) {
		error("Failed to allocate");
		return 0;
	}
	block->packet = malloc(MAXBUF);
	if (block->packet == 0) {
		error("Failed to allocate packet buffer");
		goto err;
	}

	//char filter[196];
	//sprintf(filter, "(ip.SrcAddr >= %s and ip.SrcAddr <= %s and ip.DstAddr >= %s and ip.DstAddr <= %s)", start_src_ip, end_src_ip, start_dst_ip, end_dst_ip);
	block->windivert = WinDivertOpen(filter, layer, IP_BLOCK_PRIORITY, 0);
	if (block->windivert == INVALID_HANDLE_VALUE)
	{
		error("Failed to open the WinDivert device (%d)", GetLastError());
		goto err;
	}
	block->thread_in = CreateThread(NULL, 256 * 1024, block_ip_thread, block, 0, 0);
	if (block->thread_in == 0) {
		error("Failed to create thread (%d)", GetLastError());
		goto err;
	}
	return block;
err:
	if (block->packet != 0) {
		free(block->packet);
	}
	if (block->windivert != INVALID_HANDLE_VALUE && block->windivert != 0)
	{
		WinDivertClose(block->windivert);
	}
	free(block);
	return 0;
}
#include <jni.h>
JNIEXPORT jlong JNICALL Java_net_java_faker_WinRedirect_redirectStart(JNIEnv* env, jclass cl, jint redirect_port, jstring jFilter, jint layer) {
	if (jFilter != NULL) {
		const char* filter = (*env)->GetStringUTFChars(env, jFilter, 0);
		jlong jRedirect = (jlong)redirect_start((UINT16)redirect_port, (char*)filter, (char)layer);
		(*env)->ReleaseStringUTFChars(env, jFilter, filter);
		return jRedirect;
	}
	return (jlong)redirect_start((UINT16)redirect_port, NULL, (char)layer);
}

JNIEXPORT void JNICALL Java_net_java_faker_WinRedirect_redirectStop(JNIEnv* env, jclass cl, jlong jRedirect) {
	redirect_stop((PREDIRECT)jRedirect);
}

JNIEXPORT void JNICALL Java_net_java_faker_WinRedirect_redirectPause(JNIEnv* env, jclass cl, jlong jRedirect) {
	redirect_pause((PREDIRECT)jRedirect);
}

JNIEXPORT void JNICALL Java_net_java_faker_WinRedirect_redirectResume(JNIEnv* env, jclass cl, jlong jRedirect) {
	redirect_resume((PREDIRECT)jRedirect);
}

JNIEXPORT jboolean JNICALL Java_net_java_faker_WinRedirect_redirectAddSkipPort(JNIEnv* env, jclass cl, jlong jRedirect, jint port) {
	return redirect_add_skip_port((PREDIRECT)jRedirect, (UINT16)port);
}

JNIEXPORT jboolean JNICALL Java_net_java_faker_WinRedirect_redirectRemoveSkipPort(JNIEnv* env, jclass cl, jlong jRedirect, jint port) {
	return redirect_remove_skip_port((PREDIRECT)jRedirect, (UINT16)port);
}

JNIEXPORT jboolean JNICALL Java_net_java_faker_WinRedirect_redirectGetRealAddresses(JNIEnv* env, jclass cl, jlong jRedirect, jstring jip, jint port, jobjectArray out) {
	if (jip == NULL || out == NULL) {
		return FALSE;
	}
	const char* ip = (*env)->GetStringUTFChars(env, jip, 0);
	UINT32 RealSrcIp;
	UINT16 RealSrcPort;
	UINT32 RealDstIp;
	UINT16 RealDstPort;
	BOOL ret = redirect_get_real_addresses((PREDIRECT)jRedirect, (char*)ip, port, &RealSrcIp, &RealSrcPort, &RealDstIp, &RealDstPort);
	(*env)->ReleaseStringUTFChars(env, jip, ip);
	if (ret) {
		jclass InetSocketAddress = (*env)->FindClass(env, "java/net/InetSocketAddress");
		jmethodID constructor = (*env)->GetMethodID(env, InetSocketAddress, "<init>", "(Ljava/lang/String;I)V");
		jobject srcAddr = (*env)->NewObject(env, InetSocketAddress, constructor, (*env)->NewStringUTF(env, ntoa(RealSrcIp)), ntohs(RealSrcPort));
		jobject dstAddr = (*env)->NewObject(env, InetSocketAddress, constructor, (*env)->NewStringUTF(env, ntoa(RealDstIp)), ntohs(RealDstPort));
		(*env)->SetObjectArrayElement(env, out, 0, srcAddr);
		(*env)->SetObjectArrayElement(env, out, 1, dstAddr);
	}

	return ret;
}

JNIEXPORT jint JNICALL Java_net_java_faker_WinRedirect_redirectGetActiveConnectionsCount(JNIEnv* env, jclass cl, jlong jRedirect) {
	return redirect_get_active_connections_count((PREDIRECT)jRedirect);
}

JNIEXPORT jlong JNICALL Java_net_java_faker_WinRedirect_mdnsLlmnrDisable(JNIEnv* env, jclass cl, jstring jip) {
	if (jip != NULL) {
		const char* ip = (*env)->GetStringUTFChars(env, jip, 0);
		jlong jRedirect = (jlong)mdns_llmnr_disable((char*)ip);
		(*env)->ReleaseStringUTFChars(env, jip, ip);
		return jRedirect;
	}
	return (jlong)mdns_llmnr_disable(NULL);
}

JNIEXPORT jboolean JNICALL Java_net_java_faker_WinRedirect_mdnsLlmnrRestore(JNIEnv* env, jclass cl, jlong jmdns) {
	return mdns_restore((PMDNS)jmdns);
}
JNIEXPORT jboolean JNICALL Java_net_java_faker_WinRedirect_enableTtlFix(JNIEnv* env, jclass cl) {
	return enable_ttl_fix();
}

JNIEXPORT jboolean JNICALL Java_net_java_faker_WinRedirect_disableTtlFix(JNIEnv* env, jclass cl) {
	return disable_ttl_fix();
}

JNIEXPORT jlong JNICALL Java_net_java_faker_WinRedirect_portForwardStart(JNIEnv* env, jclass cl, jstring listenIp, jstring listenStartIp, jstring listenEndIp, jstring toIp, jstring thisIp, jintArray skipPorts) {
	if (listenIp == 0 || listenStartIp == 0 || listenEndIp == 0 || toIp == 0 || thisIp == 0) {
		error("null");
		return 0;
	}
	jlong port_forward = 0;
	const char* listen_ip = (*env)->GetStringUTFChars(env, listenIp, 0);
	const char* listen_start_ip = (*env)->GetStringUTFChars(env, listenStartIp, 0);
	const char* listen_end_ip = (*env)->GetStringUTFChars(env, listenEndIp, 0);
	const char* to_ip = (*env)->GetStringUTFChars(env, toIp, 0);
	const char* this_ip = (*env)->GetStringUTFChars(env, thisIp, 0);
	jint skip_port_count = 0;
	UINT16* skip_ports = 0;
	if (skipPorts != NULL) {
		skip_port_count = (*env)->GetArrayLength(env, skipPorts);
		if (skip_port_count > 0) {
			skip_ports = malloc(skip_port_count * sizeof(UINT16));
			if (skip_ports == 0) {
				error("Failed to allocate skip ports");
				goto cleanup;
			}
			jint* ports = (*env)->GetIntArrayElements(env, skipPorts, 0);
			for (INT32 i = 0; i < skip_port_count; i++) {
				skip_ports[i] = (UINT16)ports[i];
			}
			(*env)->ReleaseIntArrayElements(env, skipPorts, ports, 0);
		}
	}
	port_forward = (jlong)port_forward_start((char*)listen_ip, (char*)listen_start_ip, (char*)listen_end_ip, (char*)to_ip, (char*)this_ip, skip_ports, (UINT16)skip_port_count);

cleanup:
	if (skip_ports != 0) {
		free(skip_ports);
	}
	(*env)->ReleaseStringUTFChars(env, listenIp, listen_ip);
	(*env)->ReleaseStringUTFChars(env, listenStartIp, listen_start_ip);
	(*env)->ReleaseStringUTFChars(env, listenEndIp, listen_end_ip);
	(*env)->ReleaseStringUTFChars(env, toIp, to_ip);
	(*env)->ReleaseStringUTFChars(env, thisIp, this_ip);
	return port_forward;
}
JNIEXPORT jboolean JNICALL Java_net_java_faker_WinRedirect_portForwardStop(JNIEnv* env, jclass cl, jlong jportForward) {
	return port_forward_stop((PFORWARD)jportForward);

}
JNIEXPORT jlong JNICALL Java_net_java_faker_WinRedirect_firewallStart(JNIEnv* env, jclass cl, jstring jfilter, jint layer) {
	if (jfilter == 0) {
		error("null");
		return 0;
	}
	const char* filter = (*env)->GetStringUTFChars(env, jfilter, 0);

	jlong block_ip = (jlong)block_ip_start(filter, layer);
	(*env)->ReleaseStringUTFChars(env, jfilter, filter);

	return block_ip;
}

JNIEXPORT jboolean JNICALL Java_net_java_faker_WinRedirect_firewallStop(JNIEnv* env, jclass cl, jlong jblockIp) {
	return block_ip_stop((PBLOCK_IP)jblockIp);
}

JNIEXPORT jstring JNICALL Java_net_java_faker_WinRedirect_getError(JNIEnv* env, jclass cl) {
	return (*env)->NewStringUTF(env, get_error());
}

JNIEXPORT void JNICALL Java_net_java_faker_WinRedirect_resetError(JNIEnv* env, jclass cl) {
	reset_error();
}

JNIEXPORT void JNICALL Java_net_java_faker_WinRedirect_setLogLevel(JNIEnv* env, jclass cl, jint level) {
	set_log_level(level);
}

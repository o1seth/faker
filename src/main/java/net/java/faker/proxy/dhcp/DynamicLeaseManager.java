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

package net.java.faker.proxy.dhcp;

import net.java.faker.util.logging.Logger;
import net.java.faker.util.network.NetworkUtil;
import org.anarres.dhcp.common.address.NetworkAddress;
import org.anarres.dhcp.common.address.Subnet;
import org.apache.directory.server.dhcp.DhcpException;
import org.apache.directory.server.dhcp.io.DhcpRequestContext;
import org.apache.directory.server.dhcp.messages.DhcpMessage;
import org.apache.directory.server.dhcp.messages.HardwareAddress;
import org.apache.directory.server.dhcp.options.vendor.DomainName;
import org.apache.directory.server.dhcp.options.vendor.DomainNameServers;
import org.apache.directory.server.dhcp.options.vendor.Routers;
import org.apache.directory.server.dhcp.options.vendor.SubnetMask;
import org.apache.directory.server.dhcp.service.manager.AbstractDynamicLeaseManager;
import org.apache.directory.server.dhcp.service.store.Lease;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class DynamicLeaseManager extends AbstractDynamicLeaseManager {
    Routers routers;
    DomainNameServers nameServers;
    SubnetMask mask;
    Map<HardwareAddress, Lease> leases = new HashMap<>();
    Inet4Address startAddress;
    Inet4Address endAddress;
    int start;
    int end;
    int next;

    public DynamicLeaseManager(String startAddress, String endAddress, Routers routers, DomainNameServers nameServers, SubnetMask mask) throws UnknownHostException {
        this((Inet4Address) InetAddress.getByName(startAddress), (Inet4Address) InetAddress.getByName(endAddress), routers, nameServers, mask);
    }

    public DynamicLeaseManager(Inet4Address startAddress, Inet4Address endAddress, Routers routers, DomainNameServers nameServers, SubnetMask mask) {
        this.startAddress = startAddress;
        this.endAddress = endAddress;
        this.routers = routers;
        this.nameServers = nameServers;
        this.mask = mask;
        TTL_LEASE.minLeaseTime = 3600;
        TTL_LEASE.maxLeaseTime = 86400 * 3;
        TTL_LEASE.defaultLeaseTime = 86400;

        TTL_OFFER.minLeaseTime = 3600;
        TTL_OFFER.maxLeaseTime = 86400 * 3;
        TTL_OFFER.defaultLeaseTime = 86400;
        this.start = NetworkUtil.getIntAddress(this.startAddress);
        this.end = NetworkUtil.getIntAddress(this.endAddress);
        this.next = start;
    }

    @CheckForNull
    @Override
    protected InetAddress getFixedAddressFor(@Nonnull HardwareAddress hardwareAddress) throws DhcpException {
        throw new DhcpException("getFixedAddressFor not implemented");
    }

    @CheckForNull
    @Override
    protected Subnet getSubnetFor(@Nonnull NetworkAddress networkAddress) throws DhcpException {
        throw new DhcpException("getSubnetFor not implemented");
    }

    @CheckForNull
    @Override
    protected boolean leaseIp(@Nonnull InetAddress address, @Nonnull HardwareAddress hardwareAddress, long ttl) throws Exception {
        throw new DhcpException("leaseIp not implemented");
    }

    private Inet4Address nextAddress() throws DhcpException {
        while (this.next <= this.end) {
            Inet4Address nextAddress = NetworkUtil.fromIntAddress(this.next);
            this.next++;
            if (NetworkUtil.localIpExists(nextAddress.getHostAddress())) {
                continue;
            }
            return nextAddress;
        }
        long currentSeconds = System.currentTimeMillis() / 1000;
        Iterator<Map.Entry<HardwareAddress, Lease>> iterator = this.leases.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<HardwareAddress, Lease> e = iterator.next();
            if (e.getValue().getExpires() < currentSeconds) {
                iterator.remove();
                Inet4Address address = (Inet4Address) e.getValue().getClientAddress();
                if (!NetworkUtil.localIpExists(address.getHostAddress())) {
                    return address;
                }
            }
        }
        throw new DhcpException("No free leases");
    }

    private Lease newLease(HardwareAddress hardwareAddress, long ttl) throws DhcpException {
        long currentSeconds = System.currentTimeMillis() / 1000;
        Lease lease = new Lease(hardwareAddress, nextAddress());
        lease.setExpires(currentSeconds + ttl);
        return lease;
    }

    @CheckForNull
    @Override
    protected InetAddress leaseMac(@Nonnull DhcpRequestContext context, @Nonnull DhcpMessage request, @CheckForNull InetAddress clientRequestedAddress, long ttl) throws Exception {
        HardwareAddress hardwareAddress = request.getHardwareAddress();
        Lease lease = leases.get(hardwareAddress);
//        if (lease != null) {
//            long currentSeconds = System.currentTimeMillis() / 1000;
//            if (lease.getExpires() < currentSeconds) {
//                leases.remove(hardwareAddress);
//                lease = null;
//            }
//        }
        if (lease == null) {
            lease = newLease(hardwareAddress, ttl);
            this.leases.put(hardwareAddress, lease);
            Logger.info("<DHCP> new lease " + lease.getHardwareAddress() + " " + lease.getClientAddress());
        } else {
            Logger.info("<DHCP> old lease " + lease.getHardwareAddress() + " " + lease.getClientAddress());
        }

        return lease.getClientAddress();
    }

    @Override
    public DhcpMessage leaseRequest(DhcpRequestContext context, DhcpMessage request, InetAddress clientRequestedAddress, long clientRequestedExpirySecs) throws DhcpException {
        Logger.debug(" request " + request + " " + clientRequestedAddress);
        DhcpMessage reply = super.leaseRequest(context, request, clientRequestedAddress, clientRequestedExpirySecs);
        if (reply == null) {
            return null;
        }
        reply.getOptions().add(routers);
        reply.getOptions().add(nameServers);
//        DomainName domainName = new DomainName();
//        domainName.setStringValue("");
//        reply.getOptions().add(domainName);
        if (mask != null) {
            reply.getOptions().add(mask);
        }
        Logger.debug(" request reply " + reply);
        return reply;
    }

    @Override
    public DhcpMessage leaseOffer(DhcpRequestContext context, DhcpMessage request, InetAddress clientRequestedAddress, long clientRequestedExpirySecs) throws DhcpException {
        Logger.debug(" offer " + request + " " + clientRequestedAddress + " clientRequestedExpirySecs " + clientRequestedExpirySecs);

        DhcpMessage reply = super.leaseOffer(context, request, clientRequestedAddress, clientRequestedExpirySecs);
        if (reply == null) {
            return null;
        }
        reply.getOptions().add(routers);
        reply.getOptions().add(nameServers);
        if (mask != null) {
            reply.getOptions().add(mask);
        }
        Logger.debug(" offer reply " + reply);
        return reply;
    }
}

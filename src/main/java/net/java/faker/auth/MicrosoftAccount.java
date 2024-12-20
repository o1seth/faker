package net.java.faker.auth;

import com.google.gson.JsonObject;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.java.StepMCProfile;
import net.raphimc.minecraftauth.step.java.StepPlayerCertificates;
import net.raphimc.minecraftauth.step.java.session.StepFullJavaSession;

import java.util.UUID;

public class MicrosoftAccount extends Account {

//    public static final AbstractStep<?, StepFullJavaSession.FullJavaSession> DEVICE_CODE_LOGIN = MinecraftAuth.builder()
//            .withClientId(MicrosoftConstants.JAVA_TITLE_ID).withScope(MicrosoftConstants.SCOPE_TITLE_AUTH)
//            .deviceCode()
//            .withDeviceToken("Win32")
//            .sisuTitleAuthentication(MicrosoftConstants.JAVA_XSTS_RELYING_PARTY)
//            .buildMinecraftJavaProfileStep(true);

    private StepFullJavaSession.FullJavaSession javaSession;

//    public MicrosoftAccount(final JsonObject jsonObject) {
//        this.javaSession = DEVICE_CODE_LOGIN.fromJson(jsonObject.getAsJsonObject("javaSession"));
//    }

    public MicrosoftAccount(final StepFullJavaSession.FullJavaSession javaSession) {
        this.javaSession = javaSession;
    }

    @Override
    public JsonObject toJson() {
//        final JsonObject jsonObject = new JsonObject();
//        jsonObject.add("javaSession", DEVICE_CODE_LOGIN.toJson(this.javaSession));
//        return jsonObject;
        return MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.toJson(javaSession);
    }


    @Override
    public String getName() {
        return this.javaSession.getMcProfile().getName();
    }

    @Override
    public UUID getUUID() {
        return this.javaSession.getMcProfile().getId();
    }

    public StepMCProfile.MCProfile getMcProfile() {
        return this.javaSession.getMcProfile();
    }

    public StepPlayerCertificates.PlayerCertificates getPlayerCertificates() {
        return this.javaSession.getPlayerCertificates();
    }

    @Override
    public String getDisplayString() {
        return this.getName() + " (Microsoft)";
    }

    @Override
    public boolean refresh() throws Exception {
        if (!super.refresh()) return false;

//        this.javaSession = DEVICE_CODE_LOGIN.refresh(MinecraftAuth.createHttpClient(), this.javaSession);
        this.javaSession = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.refresh(MinecraftAuth.createHttpClient(), this.javaSession);
        return true;
    }

    @Override
    public String toString() {
        return this.getName();
    }
}

-dontwarn **
-renamesourcefileattribute null
-keep class io.netty.** { *; }
-keep class org.cloudburstmc.netty.** { *; }
-keep @io.netty.channel.ChannelHandler$Sharable class *
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class coelho.msftauth.api.** { *; }
-keep class com.project.lumina.client.util.** { *; }
-keep class com.project.lumina.client.constructors.AccountManager { *; }
-keep class com.project.lumina.relay.** { *; }
-keep class org.cloudburstmc.protocol.** { *; }
-keep class com.mycompany.application.** { *; }
-keep class com.project.lumina.client.CPPBridge.**  { *; }
-keep class com.project.lumina.client.constructors.ModuleManager.** {*;}
-keep class com.project.lumina.client.constructors.GameDataManager.** {*;}
-keep class net.raphimc.** {*;}
-keep class io.jsonwebtoken.** {*;}


package org.apache.dubbo.remoting;
import org.apache.dubbo.common.extension.ExtensionLoader;
public class Codec2$Adaptive implements org.apache.dubbo.remoting.Codec2 {
public java.lang.Object decode(org.apache.dubbo.remoting.Channel arg0, org.apache.dubbo.remoting.buffer.ChannelBuffer arg1) throws java.io.IOException {
if (arg0 == null) throw new IllegalArgumentException("org.apache.dubbo.remoting.Channel argument == null");
if (arg0.getUrl() == null) throw new IllegalArgumentException("org.apache.dubbo.remoting.Channel argument getUrl() == null");
org.apache.dubbo.common.URL url = arg0.getUrl();
String extName = url.getParameter("codec", "adaptive");
if(extName == null) throw new IllegalStateException("Failed to get extension (org.apache.dubbo.remoting.Codec2) name from url (" + url.toString() + ") use keys([codec])");
org.apache.dubbo.remoting.Codec2 extension = (org.apache.dubbo.remoting.Codec2)ExtensionLoader.getExtensionLoader(org.apache.dubbo.remoting.Codec2.class).getExtension(extName);
return extension.decode(arg0, arg1);
}
public void encode(org.apache.dubbo.remoting.Channel arg0, org.apache.dubbo.remoting.buffer.ChannelBuffer arg1, java.lang.Object arg2) throws java.io.IOException {
if (arg0 == null) throw new IllegalArgumentException("org.apache.dubbo.remoting.Channel argument == null");
if (arg0.getUrl() == null) throw new IllegalArgumentException("org.apache.dubbo.remoting.Channel argument getUrl() == null");
org.apache.dubbo.common.URL url = arg0.getUrl();
String extName = url.getParameter("codec", "adaptive");
if(extName == null) throw new IllegalStateException("Failed to get extension (org.apache.dubbo.remoting.Codec2) name from url (" + url.toString() + ") use keys([codec])");
org.apache.dubbo.remoting.Codec2 extension = (org.apache.dubbo.remoting.Codec2)ExtensionLoader.getExtensionLoader(org.apache.dubbo.remoting.Codec2.class).getExtension(extName);
extension.encode(arg0, arg1, arg2);
}
}
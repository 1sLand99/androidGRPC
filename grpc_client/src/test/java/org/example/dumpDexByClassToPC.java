package org.example;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class dumpDexByClassToPC {

    public static void main(String[] args) {

        String currentDir = System.getProperty("user.dir");
        String dumpWork = currentDir+"/dumpWork";
        String host = "192.168.11.121";      //remote android ip
        int port = 9091;
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().maxInboundMessageSize(Integer.MAX_VALUE).build();
        GrpcService service = new GrpcService(channel);
        service.dumpDexByClass(dumpWork,"site.kos.loader.b");

    }
}

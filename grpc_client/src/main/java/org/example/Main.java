package org.example;

import com.google.protobuf.InvalidProtocolBufferException;
import com.kone.pbdemo.protocol.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.Iterator;

// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.
public class Main {


    public class a {

        public void a(){
        }
        public void a(String p0){
        }
        public void b(int p0){
        }

    }


    public static void main(String[] args) throws InterruptedException {
        FileOuterClass.File file = FileOuterClass.File.newBuilder()
                .setName("fileName")
                .setSize(200)
                .build();
        User user = User.newBuilder()
                .setUsername("zhangsan")
                .setUserId(100)
                .putHobbys("pingpong", "play pingpong")
                .setCode(200)
                .setFile(file)
//                .setError("error string")
                .build();

        System.out.println(user);
        System.out.println(user.getMsgCase().getNumber());


//        测试反序列化
        try {
            FileOuterClass.File fileNew = FileOuterClass.File.parseFrom(file.toByteArray());
            System.out.println(fileNew);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }

        String host = "192.168.31.152";
        int port = 9091;
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();

        UserServiceGrpc.UserServiceBlockingStub userServiceBlockingStub = UserServiceGrpc.newBlockingStub(channel);

        Empty empty = Empty.newBuilder().build();
//        StringArgument className = StringArgument.newBuilder().setClassName("com.yunmai.valueoflife.MainActivity$a").build();
        StringArgument className = StringArgument.newBuilder().setClassName("com.ccb.start.MainActivity").build();

        userServiceBlockingStub.dumpdex(empty);
        userServiceBlockingStub.dumpClass(className);

//        User responseUser = userServiceBlockingStub.getUser(user);
//        System.out.println(responseUser);
//
//
//        Iterator<User> users = userServiceBlockingStub.getUsers(user);
//        while (users.hasNext()) {
//            System.out.println(users.next());
//        }
//
//
//        UserServiceGrpc.UserServiceStub userServiceStub = UserServiceGrpc.newStub(channel);
//
//        StreamObserver<User> responseObserver = new StreamObserver<User>() {
//            @Override
//            public void onNext(User user) {
//                System.out.println("responseObserver -------> ");
//                System.out.println(user);
//            }
//
//            @Override
//            public void onError(Throwable throwable) {
//                System.out.println("responseObserver onError " + throwable.getMessage());
//            }
//
//            @Override
//            public void onCompleted() {
//                System.out.println("responseObserver onCompleted");
//            }
//        };
//        StreamObserver<User> requestObserver = userServiceStub.saveUsers(responseObserver);
//
//        for (int i = 0; i < 3; i++) {
//            requestObserver.onNext(user);
//        }
//
//        requestObserver.onCompleted();

        Thread.sleep(2000);
        channel.shutdown();
    }
}
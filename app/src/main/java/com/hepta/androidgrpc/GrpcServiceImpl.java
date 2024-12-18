package com.hepta.androidgrpc;

import static com.hepta.androidgrpc.dump.getDexBuffbyCookieLong;

import android.content.Context;
import android.util.Log;


import com.google.protobuf.ByteString;
import hepta.dump.protocol.DexClassLoaderInfo;
import hepta.dump.protocol.DexClassLoaders;
import hepta.dump.protocol.DexFilePoint;

import hepta.dump.protocol.DumpClassInfo;
import hepta.dump.protocol.DumpMethodInfo;
import hepta.dump.protocol.Empty;
import hepta.dump.protocol.StringArgument;
import hepta.dump.protocol.StringList;
import hepta.dump.protocol.UserServiceGrpc;
import hepta.dump.protocol.MEMbuff;
import hepta.dump.protocol.DumpMethodString;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import hepta.dump.protocol.DumpMemInfo;
import io.grpc.stub.StreamObserver;


public class GrpcServiceImpl extends UserServiceGrpc.UserServiceImplBase {


    public Context context;
    public String source;
    public String argument;
    public ClassLoader [] classLoaders;
    public GrpcServiceImpl(Context ctx,String source, String argument){
        dump.Entry(ctx,source,argument);
        context = ctx;
        this.source = source;
        this.argument = argument;
    }

    public Class<?> AndroidFindClass(String name){
        if (classLoaders == null) {
            classLoaders = dump.getClassLoaderList();
        }
        for (ClassLoader classLoader:classLoaders){
            try {
                return classLoader.loadClass(name);
            } catch (ClassNotFoundException ignored) {

            }
        }
        return null;
    }

    @Override
    public void dexDumpToLocal(Empty request, StreamObserver<Empty> responseObserver) {
        dump.Entry(context,source,argument);
        dump.dumpdexToLocal(context);
        responseObserver.onNext(request);
        responseObserver.onCompleted();
    }


    @Override
    public void getDexClassLoaderInfoByClass(StringArgument request, StreamObserver<DexClassLoaderInfo> responseObserver) {
        String clsName = request.getStringContent();
        Class<?> cls = AndroidFindClass(clsName);
        DexClassLoaderInfo.Builder dexClassLoaderCookie_build = DexClassLoaderInfo.newBuilder();
        if(cls != null) {
            Log.e("rzx", "Found Class :" + cls.getName());
            if (cls.getName().equals(clsName)) {
                AndroidClassLoaderInfo androidClassLoaderInfo = dump.getClassLoaderCookie(context, cls);
                assert androidClassLoaderInfo != null;
                for (long dexCookie : androidClassLoaderInfo.cookie) {
                    dexClassLoaderCookie_build.setStatus(true);
                    dexClassLoaderCookie_build.addValues(dexCookie);
                    dexClassLoaderCookie_build.setDexpath(androidClassLoaderInfo.FilePatch);
                    dexClassLoaderCookie_build.setClassLoadType(androidClassLoaderInfo.ClassType);
                }
                responseObserver.onNext(dexClassLoaderCookie_build.build());
                responseObserver.onCompleted();
                return;
            }

        }
        dexClassLoaderCookie_build.setStatus(false);
        dexClassLoaderCookie_build.setMsg(clsName+" not found");


        responseObserver.onNext(dexClassLoaderCookie_build.build());
        responseObserver.onCompleted();
    }


    @Override
    public void getDexClassLoaderList(Empty request, StreamObserver<DexClassLoaders> responseObserver) {
        List<AndroidClassLoaderInfo> dexClassLoaderCookieList = dump.getDexClassLoaderCookieList(context);
        DexClassLoaders.Builder dexClassLoaderInfoList_build =  DexClassLoaders.newBuilder();
        for(AndroidClassLoaderInfo androidClassLoaderInfo:dexClassLoaderCookieList ){
            DexClassLoaderInfo.Builder dexClassLoaderCookie_build = DexClassLoaderInfo.newBuilder();
            for(long dexCookie:androidClassLoaderInfo.cookie){
                dexClassLoaderCookie_build.addValues(dexCookie);
            }
            dexClassLoaderCookie_build.setDexpath(androidClassLoaderInfo.FilePatch);
            dexClassLoaderCookie_build.setClassLoadType(androidClassLoaderInfo.ClassType);
            dexClassLoaderInfoList_build.addDexClassLoadInfo(dexClassLoaderCookie_build.build());
        }
        responseObserver.onNext(dexClassLoaderInfoList_build.build());
        responseObserver.onCompleted();
    }

    @Override
    public void dexDumpByDexFilePoint(DexFilePoint request, StreamObserver<MEMbuff> responseObserver) {
        long dexFilePoint =  request.getValues();
        byte[] buff = getDexBuffbyCookieLong(dexFilePoint);
        MEMbuff dexbuff = MEMbuff.newBuilder().setContent(ByteString.copyFrom(buff)).build();
        responseObserver.onNext(dexbuff);
        responseObserver.onCompleted();
    }


    @Override
    public void dumpClass(StringArgument request, StreamObserver<DumpClassInfo> responseObserver) {
        String clsName = request.getStringContent();
        DumpClassInfo.Builder classInfobuilder = DumpClassInfo.newBuilder();

//        Log.e("rzx","dumpClass:"+clsName);
        Class<?> cls = AndroidFindClass(clsName);

        if(cls != null){
            classInfobuilder.setStatus(true);
            try {
                Method[] Declaredmethods =  cls.getDeclaredMethods();   //有些类会出现找到类，但是又报错java.lang.NoClassDefFoundError:
                for (Method method :Declaredmethods ) {
                    int modifiers = method.getModifiers();
                    if(Modifier.isNative(modifiers)){
                        continue;
                    }
                    byte[] MethodCodeItem = dump.dumpMethodByMember(method);
                    DumpMethodInfo.Builder methodInfobuilder = DumpMethodInfo.newBuilder();
                    if(MethodCodeItem == null){
                        methodInfobuilder.setStatus(false);
                        classInfobuilder.addDumpMethodInfo(methodInfobuilder.build());
                    }else {
                        String jniSignature = JNISignatureConverter.convertToJNISignature(method);
                        methodInfobuilder.setMethodName(method.getName());
                        methodInfobuilder.setMethodSign(jniSignature);
                        methodInfobuilder.setStatus(true);
                        methodInfobuilder.setContent(ByteString.copyFrom(MethodCodeItem));
                        classInfobuilder.addDumpMethodInfo(methodInfobuilder.build());
                    }
                }

            }catch (NoClassDefFoundError e){
                Log.e("Rzx", Objects.requireNonNull(e.getMessage()));

            }
            try {
                Constructor[] DeclaredConstructors = cls.getDeclaredConstructors();
                for (Constructor method : DeclaredConstructors) {
                    byte[] MethodCodeItem = dump.dumpMethodByMember(method);
                    DumpMethodInfo.Builder methodInfobuilder = DumpMethodInfo.newBuilder();
                    String jniSignature = JNISignatureConverter.convertToJNISignature(method);
                    methodInfobuilder.setMethodName(method.getName());
                    methodInfobuilder.setMethodSign(jniSignature);
                    methodInfobuilder.setContent(ByteString.copyFrom(MethodCodeItem));
                    classInfobuilder.addDumpMethodInfo(methodInfobuilder.build());
                }
            }catch (NoClassDefFoundError e){
                Log.e("Rzx", Objects.requireNonNull(e.getMessage()));
            }
        }else {
            classInfobuilder.setStatus(false);
        }

        responseObserver.onNext(classInfobuilder.build());
        responseObserver.onCompleted();
    }



    @Override
    public void dumpMethod(DumpMethodString request, StreamObserver<MEMbuff> responseObserver) {
        String clsName = request.getClassName();
        Class<?> cls = AndroidFindClass(clsName);
        String methodName = request.getMethodName();
        String methodSign = request.getMethodSign();
        byte[] method_code_item_buff =  dump.dumpMethodByString(cls,methodName,methodSign);
        MEMbuff buff = MEMbuff.newBuilder().setContent(ByteString.copyFrom(method_code_item_buff)).build();
        responseObserver.onNext(buff);
        responseObserver.onCompleted();
    }

    @Override
    public void getCurrentPackageName(Empty request, StreamObserver<StringArgument> responseObserver) {
        StringArgument stringArgument = StringArgument.newBuilder().setStringContent(context.getPackageName()).build();
        responseObserver.onNext(stringArgument);
        responseObserver.onCompleted();
    }

    @Override
    public void dumpSoMemByName(StringArgument request, StreamObserver<MEMbuff> responseObserver) {
        String soName =  request.getStringContent();
        byte[] som_mem_buff =  dump.dumpSoMemByName(soName);
        MEMbuff buff = MEMbuff.newBuilder().setContent(ByteString.copyFrom(som_mem_buff)).build();
        responseObserver.onNext(buff);
        responseObserver.onCompleted();
    }

    @Override
    public void getSoNameList(Empty request, StreamObserver<StringList> responseObserver) {
        List<String> soNameList = Arrays.asList(dump.getSoNameList());
        StringList stringList = StringList.newBuilder().addAllStrlist(soNameList).build();
        responseObserver.onNext(stringList);
        responseObserver.onCompleted();
    }

    @Override
    public void dumpMemByaddr(DumpMemInfo request, StreamObserver<MEMbuff> responseObserver) {
        long address = request.getAddress();
        long size = request.getDumpsze();
        byte[] dump_mem_buff = dump.dumpMemByaddr(address,size);
        MEMbuff buff = MEMbuff.newBuilder().setContent(ByteString.copyFrom(dump_mem_buff)).build();
        responseObserver.onNext(buff);
        responseObserver.onCompleted();
    }
}
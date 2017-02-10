package com.jude.beam.model;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.HashMap;

public class ModelManager {
    
    private final static HashMap<Class<?>,AbsModel> mModelMap = new HashMap<>();
    private final static BackThread mBackThread = new BackThread();
    private static Context mApplication;


    /**
     * 在mainifests里注册Model 注册的Model会在Application启动时初始化。
     * 没有注册就是懒加载，会在第一次使用时初始化。 在application的节点下加入，例：
     * <meta-data
            android:name="MODEL"
            android:value="com.jude.beamdemo.model.QuestionModel,
            com.jude.beamdemo.model.AccountModel"       />
     * @param ctx
     */
    public static void init(final Context ctx){
        mBackThread.start();
        mApplication = ctx;
        Class<?>[] models = null;
        try {
            ApplicationInfo appInfo = ctx.getPackageManager()
                    .getApplicationInfo(ctx.getPackageName(),
                            PackageManager.GET_META_DATA);
            if (appInfo.metaData == null||appInfo.metaData.getString("MODEL") == null||appInfo.metaData.getString("MODEL").isEmpty()){
                //Log.e("Beam","MODEL No Found!Have you declare MODEL in the manifests?");
                return;
            }
            String modelStr = appInfo.metaData.getString("MODEL").trim();
            String[] modelStrs = modelStr.split(",");
            models = new Class[modelStrs.length];
            for (int i = 0; i < modelStrs.length; i++) {
                try{
                    models[i] = Class.forName(modelStrs[i].trim());
                }catch (ClassNotFoundException e){
                    Log.e("Beam",modelStrs[i].trim()+" Class No Found!");
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            return ;
        }

        for (Class m:models) {
            createModel(m);
        }
        for (AbsModel absModel : mModelMap.values()) {
            launchModel(absModel);
        }
    }

    private static <T extends AbsModel> T createModel(Class<T> clazz){
        if (clazz!=null && AbsModel.class.isAssignableFrom(clazz)){
            try {
                AbsModel instance =  clazz.newInstance();
                mModelMap.put(clazz, instance);
                return (T) instance;
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }else{
            throw new IllegalArgumentException("your model must extends AbsModel");
        }
        throw new IllegalArgumentException("your model must extends AbsModel");
    }

    private static void launchModel(@NonNull final AbsModel model){
        model.onAppCreate(mApplication);
        //后台调用
        mBackThread.execute(new Runnable() {
            @Override
            public void run() {
                model.onAppCreateOnBackThread(mApplication);
            }
        });
    }

    public static <T extends AbsModel> T  getInstance(Class<T> clazz) {
        if (mModelMap.get(clazz) == null){
            synchronized (clazz){
                launchModel(createModel(clazz));
            }
        }
        return (T) mModelMap.get(clazz);
    }

    static void runOnBackThread(Runnable runnable){
        mBackThread.execute(runnable);
    }
}

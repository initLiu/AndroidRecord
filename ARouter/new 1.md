```java

public class ARouter$$Group$$arouter implement IRouteGroup{
    @Override
    public void loadInto(Map<String, RouteMeta> atlas){
        atlas.put('/arouter/service/autowired',RouteMeta.Build(RouteType.PROVIDER,AutowiredServiceImpl.class,'/arouter/service/autowired','arouter'))
        atlas.put('/arouter/service/interceptor',RouteMeta.Build(RouteType.PROVIDER,InterceptorServiceImpl.class,'/arouter/service/interceptor','arouter'))
    }
}

public class ARouter$$Group$$test1 implement IRouteGroup{
    @Override
    public void loadInto(Map<String, RouteMeta> atlas){
        atlas.put('/test1/activity1',RouteMeta.Build(RouteType.ACTIVITY,Test1Activity1.class,'/test1/activity1','test1'))
    }
}

```

```java
public class ARouter$$Providers$$arouterapi implement IProviderGroup{
    @Override
    public void loadInto(Map<String, RouteMeta> providers){
        providers.put(AutowiredService,RouteMeta.Build(RouteType.PROVIDER,AutowiredServiceImpl.class,'/arouter/service/autowired','arouter',))
        providers.put(InterceptorService,RouteMeta.Build(RouteType.PROVIDER,InterceptorServiceImpl.class,'/arouter/service/interceptor','arouter',))
    }
}

public class ARouter$$Root$$test1 implement IRouteRoot{
    @Override
    public void loadInto(Map<String, Class<? extends IRouteGroup>> routes){
        routes.put('test1',ARouter$$Group$$test1);
    }
}

public class ARouter$$Root$$arouterapi implement IRouteRoot{
    @Override
    public void loadInto(Map<String, Class<? extends IRouteGroup>> routes){
        routes.put('arouter',ARouter$$Group$$arouter);
    }
}
```

```java
for (String className : routerMap) {
    if (className.startsWith(ROUTE_ROOT_PAKCAGE + DOT + SDK_NAME + SEPARATOR + SUFFIX_ROOT)) {//初始化root
        // This one of root elements, load root.
        ((IRouteRoot) (Class.forName(className).getConstructor().newInstance())).loadInto(Warehouse.groupsIndex);
    } else if (className.startsWith(ROUTE_ROOT_PAKCAGE + DOT + SDK_NAME + SEPARATOR + SUFFIX_INTERCEPTORS)) {//初始化interceptor
        // Load interceptorMeta
        ((IInterceptorGroup) (Class.forName(className).getConstructor().newInstance())).loadInto(Warehouse.interceptorsIndex);
    } else if (className.startsWith(ROUTE_ROOT_PAKCAGE + DOT + SDK_NAME + SEPARATOR + SUFFIX_PROVIDERS)) {//初始化provider
        // Load providerIndex
        ((IProviderGroup) (Class.forName(className).getConstructor().newInstance())).loadInto(Warehouse.providersIndex);
    }
}
```


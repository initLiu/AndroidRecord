```java
final class ServiceMethod<R, T> {
    ....
    static final class Builder<T, R> {
        final Retrofit retrofit;
        final Method method;
        final Annotation[] methodAnnotations;
        final Annotation[][] parameterAnnotationsArray;
        final Type[] parameterTypes;

        Builder(Retrofit retrofit, Method method) {
            this.retrofit = retrofit;
            this.method = method;//interface定义的请求服务器的方法
            this.methodAnnotations = method.getAnnotations();//method上的定义的注解
            this.parameterTypes = method.getGenericParameterTypes();//method中所有参数的类型
            this.parameterAnnotationsArray = method.getParameterAnnotations();//method中所有参数的注解的类型
        }

        public ServiceMethod build() {
            
        }
    }
}
```
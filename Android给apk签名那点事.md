## 工具
*   keytool 是个密钥和证书管理工具,可以用来生成证书.
*   jarsigner 工具利用密钥仓库中的信息来产生或校验 Java 存档 (JAR) 文件的数字签名

使用keytool生成证书:
```xml
keytool -genkey -keystore test.keystore  -alias test -keyalg RSA -validity 10000
```
参数解释:
1.  -genkey 产生证书文件
2.  -keystore 指定密钥库的.keystore文件中
3.  -keyalg 指定密钥的算法,这里指定为RSA(非对称密钥算法)
4.  -validity 为证书有效天数，这里我们写的是10000天
5.  -alias 产生别名

## 签名
```xml
jarsigner -verbose -keystore test.keystore -signedjar signed.apk t.apk 'test'
```
1.  -verbose：指定生成详细输出
2.  -keystore：指定数字证书存储路径
3.  -signedjar：该选项的三个参数为 签名后的apk包 未签名的apk包 数字证书别名(注意顺序)
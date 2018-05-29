```java
	public static void connect() {
		int timeout = 20 * 1000;
		int readTimeout = 10000;
		Socket socket = null;
		try {
			InetSocketAddress serverAddress = new InetSocketAddress("118.194.55.47", 80);
			socket = new Socket();
			socket.setSoTimeout(readTimeout);
			socket.setTcpNoDelay(true);
			socket.setKeepAlive(true);
			socket.connect(serverAddress, timeout);
			System.out.println("connect");
		} catch (Exception e) {
			System.out.println("connect error");
			e.printStackTrace();
		}
	}

//setSoTimeout()这个方法所设置的超时时间还未结束的时候，可以通过socket.getInputStream()
//获得的InputStream对象进行二次读取。在二次读取的时候，如果客户端如果没有进行二次请求，
//InputStream对象二次读取的时候会死锁，直到客户端二次请求时才会继续运行，
//但是一旦超过setSoTimeout()方法所设置的超时时间，
//便会抛出Java.NET.SocketTimeoutException: Read timed out异常。
//也就是说两次请求间隔时间如果超过setSoTimeout()方法设置的超时时间，就会抛出异常，结束InputStream的二次读取
```
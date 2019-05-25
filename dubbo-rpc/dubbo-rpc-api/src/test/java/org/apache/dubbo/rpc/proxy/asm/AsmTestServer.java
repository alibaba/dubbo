package org.apache.dubbo.rpc.proxy.asm;

import java.util.List;
import java.util.Map;

public interface AsmTestServer {

	void notReturn();
	
	void notThrowable() throws Throwable;
	
	int  returnInt();
	
	long returnLong();
	
	String returnObject();
	
	int[] returnIntArray();
	
	long[] returnLongArray();
	
	String[] returnObjectArray();
	
	void parameterInt(int i);
	
	void parameterLong(long l);
	
	void parameterIntArray(int[] intArray);
	
	void parameterLongArray(long[] longArray);
	
	void parameterObject(String string);
	
	void parameterObjectArray(String[] stringArray);
	
	String execute(int in , long lo , Integer integer , Long lon , String string,List<String> list , Map<String,String> map);
	
	
	
}

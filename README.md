# GDson

自动生成TypeAdapter，支持Multi Module。

使用方法：

```
Gson gson = new GsonBuilder().registerTypeAdapterFactory(new GDsonTypeAdapterFactory()).create();
```

如果是Multi Module：

```
Gson gson = new GsonBuilder()
	.registerTypeAdapterFactory(new XxxTypeAdapterFactory())
	.registerTypeAdapterFactory(new YyyTypeAdapterFactory())
	...
	.create();
```

`Xxx`和`Yyy`是Module中用了`GDsonModule`注解了的类名。

注意事项：

1. 不支持没有`public`无参构造方法的类。
2. 不支持`private`的字段，生成的`TypeAdapter`中忽略`private`字段。
3. 不支持含有`<? extends XXX>`字段的类。
4. 如果是内部类，请用`static`修饰。
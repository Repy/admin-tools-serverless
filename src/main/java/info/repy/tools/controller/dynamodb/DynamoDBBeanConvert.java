package info.repy.tools.controller.dynamodb;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.*;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DynamoDBBeanConvert {

    public static AttributeValue toAttributeValue(Object data) {
        if (data == null) {
            return AttributeValue.builder().nul(true).build();
        }
        Class cls = data.getClass();
        if (cls == Integer.class || cls == Integer.TYPE) {
            return AttributeValue.builder().n(data.toString()).build();
        } else if (cls == Byte.class || cls == Byte.TYPE) {
            return AttributeValue.builder().n(data.toString()).build();
        } else if (cls == Long.class || cls == Long.TYPE) {
            return AttributeValue.builder().n(data.toString()).build();
        } else if (cls == Float.class || cls == Float.TYPE) {
            return AttributeValue.builder().n(data.toString()).build();
        } else if (cls == Double.class || cls == Double.TYPE) {
            return AttributeValue.builder().n(data.toString()).build();
        } else if (cls == Short.class || cls == Short.TYPE) {
            return AttributeValue.builder().n(data.toString()).build();
        } else if (cls == Character.class || cls == Character.TYPE) {
            return AttributeValue.builder().s(data.toString()).build();
        } else if (cls == Boolean.class || cls == Boolean.TYPE) {
            return AttributeValue.builder().bool((Boolean) data).build();
        } else if (cls == String.class) {
            String str = (String) data;
            if (str.isEmpty()) return AttributeValue.builder().nul(true).build();
            return AttributeValue.builder().s(str).build();
        } else if (cls.isEnum()) {
            Enum e = (Enum) data;
            return AttributeValue.builder().s(e.name()).build();
        } else if (cls.isArray()) {
            List<AttributeValue> li = new ArrayList<>();
            int length = Array.getLength(data);
            for (int i = 0; i < length; i++) {
                Object a = Array.get(data, i);
                li.add(toAttributeValue(a));
            }
            return AttributeValue.builder().l(li).build();
        } else if (data instanceof List) {
            List<AttributeValue> li = new ArrayList<>();
            List<Object> list = (List<Object>) data;
            for (Object it : list) {
                li.add(toAttributeValue(it));
            }
            return AttributeValue.builder().l(li).build();
        } else {
            // Mapで返すものは別メソッド
            return AttributeValue.builder().m(toAttributeObject(data)).build();
        }
    }

    public static HashMap<String, AttributeValue> toAttributeObject(Object data) {
        if (data instanceof Map) {
            Map map = (Map) data;
            HashMap<String, AttributeValue> item = new HashMap<String, AttributeValue>();
            for (Object key : map.keySet()) {
                Object val = map.get(key);
                item.put(key.toString(), toAttributeValue(val));
            }
            return item;
        } else {
            try {
                HashMap<String, AttributeValue> item = new HashMap<>();
                Class<?> inputClass = data.getClass();
                while (inputClass != null) {
                    BeanInfo beanInfo = Introspector.getBeanInfo(inputClass);
                    for (PropertyDescriptor f : beanInfo.getPropertyDescriptors()) {
                        Object child = f.getReadMethod().invoke(data);
                        item.put(f.getName(), toAttributeValue(child));
                    }
                    inputClass = inputClass.getSuperclass();
                }
                return item;
            } catch (IllegalAccessException | IntrospectionException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static <T> T toJavaObject(Class<T> outputCls, Map<String, AttributeValue> item) {
        if (item == null) return null;
        try {
            T obj = outputCls.getConstructor(new Class<?>[]{}).newInstance();
            Class<?> inputClass = outputCls;
            while (inputClass != null) {
                BeanInfo beanInfo = Introspector.getBeanInfo(inputClass);
                for (PropertyDescriptor f : beanInfo.getPropertyDescriptors()) {
                    AttributeValue att = item.get(f.getName());
                    if (att == null || (att.nul() != null && att.nul())) {
                        if (!f.getPropertyType().isPrimitive()) {
                            f.getWriteMethod().invoke(obj, new Object[]{null});
                        }
                    } else {
                        Object val = toJavaValue(f.getPropertyType(), f.getReadMethod().getGenericReturnType(), att);
                        f.getWriteMethod().invoke(obj, att);
                    }
                }
                inputClass = inputClass.getSuperclass();
            }
            return obj;
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException | IntrospectionException | NoSuchMethodException  e) {
            throw new RuntimeException(e);
        }
    }

    public static Object toJavaValue(Class cls, Type genericType, AttributeValue val) throws IllegalAccessException {
        if (cls == Integer.class || cls == Integer.TYPE) {
            return Integer.parseInt(val.n());
        } else if (cls == Byte.class || cls == Byte.TYPE) {
            return Byte.parseByte(val.n());
        } else if (cls == Long.class || cls == Long.TYPE) {
            return Long.parseLong(val.n());
        } else if (cls == Float.class || cls == Float.TYPE) {
            return Float.parseFloat(val.n());
        } else if (cls == Double.class || cls == Double.TYPE) {
            return Double.parseDouble(val.n());
        } else if (cls == Short.class || cls == Short.TYPE) {
            return Short.parseShort(val.n());
        } else if (cls == Character.class || cls == Character.TYPE) {
            return val.s().charAt(0);
        } else if (cls == Boolean.class || cls == Boolean.TYPE) {
            return val.bool();
        } else if (cls == String.class) {
            if (val.nul() != null && val.nul().booleanValue()) return "";
            return val.s();
        } else if (cls.isEnum()) {
            return Enum.valueOf(cls, val.s());
        } else if (cls.isArray()) {
            // 未実装
            throw new UnsupportedOperationException();
        } else if (List.class.isAssignableFrom(cls)) {
            if (List.class != cls) throw new UnsupportedOperationException();
            if (!(genericType instanceof ParameterizedType)) throw new UnsupportedOperationException();
            Type[] genType = ((ParameterizedType) genericType).getActualTypeArguments();

            if (!(genType != null && genType.length == 1 && genType[0] instanceof Class))
                throw new UnsupportedOperationException();

            List<Object> retmap = new ArrayList<>();
            List<AttributeValue> li = val.l();
            for (AttributeValue lis : li) {
                retmap.add(toJavaValue((Class) genType[0], genType[0], lis));
            }
            return retmap;
        } else if (Map.class.isAssignableFrom(cls)) {
            if (Map.class != cls) throw new UnsupportedOperationException();
            if (!(genericType instanceof ParameterizedType)) throw new UnsupportedOperationException();
            Type[] genType = ((ParameterizedType) genericType).getActualTypeArguments();
            if (!(genType != null && genType.length == 2 && genType[0] instanceof Class && String.class.isAssignableFrom((Class) genType[0]) && genType[1] instanceof Class))
                throw new UnsupportedOperationException();
            Map<String, Object> retmap = new HashMap<>();
            Map<String, AttributeValue> map = val.m();
            for (String key : map.keySet()) {
                retmap.put(key, toJavaValue((Class) genType[1], genType[1], map.get(key)));
            }
            return retmap;
        } else {
            return toJavaObject(cls, val.m());
        }
    }

}

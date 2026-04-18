package test;
import java.util.*;
class LRUCache {
    private int capacity;
    private Map<Integer, Integer> map;
    private LinkedHashMap<Integer, Integer> cache;

    public LRUCache(int capacity) {
        this.capacity = capacity;
        // 使用 accessOrder = true，保证迭代顺序是访问顺序
        this.cache = new LinkedHashMap<Integer, Integer>(capacity, 0.75f, true) {
            protected boolean removeEldestEntry(Map.Entry<Integer, Integer> eldest) {
                return size() > LRUCache.this.capacity;
            }
        };
    }

    public int get(int key) {
        return cache.getOrDefault(key, -1);
    }

    public void put(int key, int value) {
        cache.put(key, value);
    }

    public static void main(String[] args) {
        LRUCache lruCache = new LRUCache(2);
        lruCache.put(1, 1); // 缓存: {1=1}
        lruCache.put(2, 2); // 缓存: {1=1, 2=2}
        System.out.println(lruCache.get(1)); // 返回 1, 缓存顺序更新为 {2=2, 1=1}
        lruCache.put(3, 3); // 插入3, 移除最久未使用的2, 缓存: {1=1, 3=3}
        System.out.println(lruCache.get(2)); // 返回 -1 (2已被移除)
        lruCache.put(4, 4); // 插入4, 移除最久未使用的1, 缓存: {3=3, 4=4}
        System.out.println(lruCache.get(1)); // 返回 -1 (1已被移除)
        System.out.println(lruCache.get(3)); // 返回 3
        System.out.println(lruCache.get(4)); // 返回 4
    }

    // 可选：打印缓存状态，方便调试
    public void printCache() {
        System.out.println(cache);
    }
}
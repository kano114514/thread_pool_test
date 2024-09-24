import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class BlockQueue<T> {
    private Deque<T> queue=new ArrayDeque<>();
    private ReentrantLock lock=new ReentrantLock();

    private Condition fullWaitSet=lock.newCondition();

    private Condition emptyWaitSet=lock.newCondition();

    private int capacity;

    public BlockQueue(int capacity){
        this.capacity=capacity;
    }

    public T poll(long timeout, TimeUnit unit){
        lock.lock();
        try{
            long nanos=unit.toNanos(timeout);
            while(queue.isEmpty()){
                try{
                    if(nanos<=0) return null;
                    nanos=emptyWaitSet.awaitNanos(nanos);//在获取锁的时候阻塞，nanos并不会减少。
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
            T task=queue.pollFirst();
            fullWaitSet.signal();
            return task;
        }
        finally {
            lock.unlock();
        }
    }

    public T take(){
        lock.lock();
        try {
            while(queue.isEmpty()){
                try {
                    emptyWaitSet.await();
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
            T task=queue.removeFirst();
            fullWaitSet.signal();
            return task;
        }
        finally {
            lock.unlock();
        }
    }

    public void put(T task){
        lock.lock();
        try {
            while(queue.size()==capacity){
                try {
                    fullWaitSet.await();
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
            queue.offerLast(task);
            emptyWaitSet.signal();
        }finally {
            lock.unlock();
        }
    }

    public int size(){
        lock.lock();
        try{
            return  queue.size();
        }finally {
            lock.unlock();
        }
    }

}

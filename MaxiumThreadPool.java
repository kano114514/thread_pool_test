import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class MaxiumThreadPool {
    private MaxiumBlockQueue<Runnable> taskQueue;

    private HashSet<Worker> workers=new HashSet<>();
    private long timeout;
    private TimeUnit timeUnit;

    private int coreSize;
    private int maxSize;

    private ReentrantLock threadWaitLock=new ReentrantLock();

    private Condition condition=threadWaitLock.newCondition();

    public void execute(Runnable task){
        synchronized (workers){
            if(workers.size()<coreSize){
                Worker worker=new Worker(task);
                System.out.println("新增核心worker："+worker);
                worker.start();
                workers.add(worker);
            }
            else{
                boolean ifPut=taskQueue.put(task);
                if(!ifPut){
                    if(workers.size()>=coreSize&&workers.size()<maxSize){
                        Worker worker=new Worker(task,false);
                        System.out.println("新增非核心线程："+worker);
                        worker.start();
                        workers.add(worker);
                    }
                    else{
                        System.out.println("丢弃任务");
                    }
                }
                else{
                    System.out.println("放置任务");
                    try{
                        threadWaitLock.lock();
                        condition.signal();
                    }
                    finally {
                        threadWaitLock.unlock();
                    }

                }
            }
        }
    }

    public MaxiumThreadPool(int coreSize, int maxSize,long timeout, TimeUnit timeUnit, int queueCapacity){
        this.coreSize=coreSize;
        this.timeout=timeout;
        this.timeUnit=timeUnit;
        this.maxSize=maxSize;
        this.taskQueue=new MaxiumBlockQueue<>(queueCapacity);

    }

    class Worker extends Thread{
        private Runnable task;
        private boolean isCore=false;
        public Worker(Runnable task){
            this.task=task;
            this.isCore=true;
        }
        public Worker(Runnable task,boolean isCore){
            this.task=task;
            this.isCore=isCore;
        }

        @Override
        public void run(){
            long time=timeUnit.toNanos(timeout);
            while(true) {
                while (task != null || (task = taskQueue.poll(timeout, timeUnit)) != null) {//这里面有一个问题，后面来的任务无法去复用之前已经创建过的线程。
                    try {
                        System.out.println((isCore?"核心线程正在执行任务":"非核心线程正在执行任务：") + task);
                        task.run();
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        task = null;
                        time=timeUnit.toNanos(timeout);
                    }
                }
                if(!this.isCore) {
                    try {
                        threadWaitLock.lock();
                        time = condition.awaitNanos(time);
                    } catch (Exception e) {
                        e.printStackTrace();
                        synchronized (workers) {
                            System.out.println("线程执行任务结束，移出线程池：" + this);
                            workers.remove(this);
                        }
                        return;
                    } finally {
                        threadWaitLock.unlock();
                    }
                    if (time <= 0) {
                        synchronized (workers) {
                            System.out.println("线程执行任务结束，移出线程池：" + this);
                            workers.remove(this);
                        }
                        break;
                    }
                }
            }
        }
    }
}

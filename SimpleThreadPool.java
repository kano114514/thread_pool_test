import java.util.HashSet;
import java.util.concurrent.TimeUnit;

public class SimpleThreadPool {
    private BlockQueue<Runnable> taskQueue;

    private HashSet<Worker> workers=new HashSet<>();
    private long timeout;
    private TimeUnit timeUnit;

    private int coreSize;

    public void execute(Runnable task){
        synchronized (workers){
            if(workers.size()<coreSize){
                Worker worker=new Worker(task);
                System.out.println("新增worker："+worker);
                worker.start();
                workers.add(worker);
            }
            else{
                taskQueue.put(task);
                System.out.println("加入任务队列："+task);
            }
        }
    }

    public SimpleThreadPool(int coreSize, long timeout, TimeUnit timeUnit, int queueCapacity){
        this.coreSize=coreSize;
        this.timeout=timeout;
        this.timeUnit=timeUnit;
        this.taskQueue=new BlockQueue<>(queueCapacity);
    }

    class Worker extends Thread{
        private Runnable task;
        public Worker(Runnable task){
            this.task=task;
        }

        @Override
        public void run(){
            while(task!=null||(task=taskQueue.poll(timeout,timeUnit))!=null){//这里面有一个问题，后面来的任务无法去复用之前已经创建过的线程。
                try {
                    System.out.println("正在执行任务："+task);
                    task.run();
                }catch (Exception e){
                    e.printStackTrace();
                }
                finally {
                    task=null;
                }
            }
            synchronized (workers){
                System.out.println("线程执行任务结束，移出线程池："+this);
                workers.remove(this);
            }
        }
    }
}

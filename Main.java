import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[]n){
        MaxiumThreadPool maxiumThreadPool =new MaxiumThreadPool(2,5,1000, TimeUnit.MILLISECONDS,3);
        for(int i=0;i<10;i++){
            int j=i;
            maxiumThreadPool.execute(()->{
                System.out.println("任务："+ j);
            });
        }

        while(true){

        }
    }
}

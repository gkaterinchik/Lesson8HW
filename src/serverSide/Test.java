package serverSide;

public class Test {
    public static  long a,c;
    public static void main(String[] args) throws InterruptedException {
        new Thread(new Runnable() {
            public void run() {
                while(true) { //бесконечно крутим
                    try {

                        Thread.sleep(4000); // 4 секунды в милисекундах
                        System.out.println("Hi!");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }



}

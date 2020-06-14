import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.functions.Action;

public class FlowableTest {


    public static void main(String[] args) {
        Flowable.fromAction(new Action() {
            @Override
            public void run() throws Throwable {

            }
        })
    }

}

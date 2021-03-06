package io.techery.janet.http.sample;

import com.google.gson.Gson;

import io.techery.janet.ActionPipe;
import io.techery.janet.HttpActionService;
import io.techery.janet.Janet;
import io.techery.janet.gson.GsonConverter;
import io.techery.janet.helper.ActionStateSubscriber;
import io.techery.janet.okhttp3.OkClient;
import io.techery.janet.http.sample.action.base.BaseAction;
import io.techery.janet.http.sample.action.TestProgressAction;
import io.techery.janet.http.sample.action.UserReposAction;
import io.techery.janet.http.sample.action.UsersAction;
import io.techery.janet.http.sample.model.User;
import io.techery.janet.http.sample.util.SampleLoggingService;
import rx.Observable;

public class HttpSample {

    private static final String API_URL = "https://api.github.com";

    public static void main(String... args) {
        Janet janet = new Janet.Builder()
                .addService(new SampleLoggingService(new HttpActionService(API_URL, new OkClient(), new GsonConverter(new Gson()))))
                .build();

        ActionPipe<UsersAction> usersPipe = janet.createPipe(UsersAction.class);
        ActionPipe<UserReposAction> userReposPipe = janet.createPipe(UserReposAction.class);

        usersPipe.observeSuccess()
                .filter(BaseAction::isSuccess)
                .subscribe(
                        action -> System.out.println("received " + action),
                        System.err::println
                );

        usersPipe.send(new UsersAction());

        usersPipe.createObservable(new UsersAction())
                .filter(state -> state.action.isSuccess())
                .flatMap(state -> Observable.<User>from(state.action.response()).first())
                .flatMap(user -> userReposPipe.createObservable(new UserReposAction(user.getLogin())))
                .subscribe(new ActionStateSubscriber<UserReposAction>()
                        .onSuccess(action -> System.out.println("repos request finished " + action))
                        .onFail((action, throwable) -> System.err.println("repos request exception " + throwable))
                );


        janet = new Janet.Builder()
                .addService(new SampleLoggingService(new HttpActionService("https://httpbin.org", new OkClient(), new GsonConverter(new Gson()))))
                .build();

        janet.createPipe(TestProgressAction.class)
                .createObservable(new TestProgressAction())
                .subscribe(new ActionStateSubscriber<TestProgressAction>()
                        .onSuccess(action -> System.out.println("request finished " + action))
                        .onProgress((action, progress) -> System.out.println(String.format("progress value:%s", progress))));

    }
}

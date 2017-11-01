import akka.NotUsed;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.pattern.Patterns;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import akka.util.Timeout;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.reflect.ClassTag$;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

public class App extends AllDirectives {

    private static ActorSystem system = ActorSystem.apply();

    public static void main(String[] args) throws Exception {

        ActorRef human = system.actorOf(Props.create(Human.class));
        ActorRef dog = system.actorOf(Props.create(Dog.class), "Dog");
        human.tell("feed", dog);

        final Http http = Http.get(system);
        final ActorMaterializer materializer = ActorMaterializer.create(system);

        //In order to access all directives we need an instance where the routes are define.
        App app = new App();

        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = app.createRoute().flow(system, materializer);
        final CompletionStage<ServerBinding> binding = http.bindAndHandle(routeFlow,
                ConnectHttp.toHost("localhost", 8080), materializer);

        System.out.println("Server online at http://localhost:8080/\nPress RETURN to stop...");
        System.in.read(); // let it run until user presses return

        binding
                .thenCompose(ServerBinding::unbind) // trigger unbinding from the port
                .thenAccept(unbound -> system.terminate()); // and shutdown when done
    }

    private Route createRoute() {
        return route(path("hello", () -> get(this::getHelloRoute)));
    }

    private Route getHelloRoute() {
        ActorRef actor = system.actorOf(Props.create(HelloRequest.class));
        final Timeout timeout = new Timeout(5, TimeUnit.SECONDS);
        final Future<?> future = Patterns.ask(actor, "Huinya", timeout);

        return this.completeOKWithFutureString(future.mapTo(ClassTag$.MODULE$.apply(Sbasibo.class)).map((sps) -> sps.getClass().toString(), system.dispatcher()));
    }
}

class HelloRequest extends AbstractActor {

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .matchAny((a) -> {
                    Future<Object> viebu = Patterns.ask(context().actorFor("akka://default/user/Dog"), new Feed(123), 3500);

                    Object result = Await.result(viebu, new Timeout(5, TimeUnit.SECONDS).duration());

                    sender().tell(result, self());
                })
                .build();
    }
}

class Feed {

    public Feed(int count) {
        this.count = count;
    }

    public int count;
}

class Sbasibo {}

class Human extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    @Override
    public AbstractActor.Receive createReceive() {
        return receiveBuilder()
                .match(String.class, (command) -> command.equals("feed"), s -> {
                    log.info("Received String message: {}", s);
                    sender().tell(new Feed(5), self());
                })
                .match(String.class, s -> log.info("Received String message: {}", s))
                .match(Sbasibo.class, s -> log.info(Sbasibo.class.getName() + sender().toString()))
                .matchAny(o -> log.info("received unknown message"))
                .build();
    }
}

class Dog extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    @Override
    public AbstractActor.Receive createReceive() {
        return receiveBuilder()
                .match(Feed.class, s -> {
                    for (int i = 0; i < s.count; i++) {
                        log.info("Gaw Gaw");
                    }

                    ActorSystem system = context().system();
                    system.scheduler()
                            .scheduleOnce(Duration.create(3, TimeUnit.SECONDS),
                                    sender(), new Sbasibo(), system.dispatcher(), self());
                })
                .match(String.class, s -> log.info("Received String message: {}", s))
                .matchAny(o -> log.info("received unknown message"))
                .build();
    }
}
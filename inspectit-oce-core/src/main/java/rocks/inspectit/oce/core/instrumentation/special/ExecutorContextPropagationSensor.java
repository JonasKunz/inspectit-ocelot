package rocks.inspectit.oce.core.instrumentation.special;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatcher;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import rocks.inspectit.oce.bootstrap.Instances;
import rocks.inspectit.oce.core.instrumentation.config.model.InstrumentationConfiguration;
import rocks.inspectit.oce.core.instrumentation.context.ContextManager;

import java.util.concurrent.Executor;

import static net.bytebuddy.matcher.ElementMatchers.*;

@Component
@DependsOn(ContextManager.BEAN_NAME)
public class ExecutorContextPropagationSensor implements SpecialSensor {

    private static final ElementMatcher<TypeDescription> EXECUTER_CLASSES_MATCHER = isSubTypeOf(Executor.class);

    private static final ElementMatcher<MethodDescription> EXECUTER_EXECUTE_METHOD_MATCHER =
            named("execute").and(takesArgument(0, Runnable.class));

    @Override
    public boolean shouldInstrument(TypeDescription type, InstrumentationConfiguration settings) {
        return settings.getSource().getSpecial().isExecutorContextPropagation() &&
                EXECUTER_CLASSES_MATCHER.matches(type);
    }

    @Override
    public boolean requiresInstrumentationChange(TypeDescription type, InstrumentationConfiguration first, InstrumentationConfiguration second) {
        return false; //if the sensor stays active it never requires changes
    }

    @Override
    public DynamicType.Builder instrument(Class<?> clazz, TypeDescription type, InstrumentationConfiguration conf, DynamicType.Builder builder) {
        return builder.visit(
                Advice.to(ExecutorAdvice.class)
                        .on(EXECUTER_EXECUTE_METHOD_MATCHER));
    }

    private static class ExecutorAdvice {
        /**
         * Wraps the given runnable of {@link java.util.concurrent.Executor#execute(Runnable)} via {@link ContextManager#wrap(Runnable)}}
         *
         * @param runnable
         */
        @Advice.OnMethodEnter
        public static void enter(@Advice.Argument(value = 0, readOnly = false) Runnable runnable) {
            runnable = Instances.contextManager.wrap(runnable);
        }
    }
}

package no.nav.aura.envconfig.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import no.nav.aura.envconfig.model.ModelEntity;

public abstract class ReflectionUtil {

    private ReflectionUtil() {
    }

    @SuppressWarnings("unchecked")
    public static List<Field> getDeclaredFieldsWithSupers(Class<? extends ModelEntity> clazz) {
        List<Field> list =  new ArrayList<>(Arrays.asList(clazz.getDeclaredFields()));
        if (ModelEntity.class.isAssignableFrom(clazz.getSuperclass())) {
            list.addAll(getDeclaredFieldsWithSupers((Class<? extends ModelEntity>) clazz.getSuperclass()));
        }
        return list;
    }

    public static void doRecursively(ModelEntity entity, Consumer<ModelEntity> effect) {
        doRecursively(entity, effect, new HashSet<Integer>());
    }

    private static void doRecursively(ModelEntity entity, Consumer<ModelEntity> effect, Set<Integer> touched) {
        try {
            if (entity == null) {
                return;
            }
            int identityHashCode = System.identityHashCode(entity);
            if (touched.contains(identityHashCode)) {
                return;
            }
            touched.add(identityHashCode);
            effect.process(entity);
            for (Field field : getDeclaredFieldsWithSupers(entity.getClass())) {
                doWithField(entity, effect, touched, field);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static void doWithField(ModelEntity entity, Consumer<ModelEntity> effect, Set<Integer> touched, Field field) throws IllegalAccessException {
        if (Collection.class.isAssignableFrom(field.getType())) {
            doWithCollection(entity, effect, touched, field);
        } else if (ModelEntity.class.isAssignableFrom(field.getType())) {
            field.setAccessible(true);
            doRecursively((ModelEntity) field.get(entity), effect, touched);
        }
    }

    private static void doWithCollection(ModelEntity entity, Consumer<ModelEntity> effect, Set<Integer> touched, Field field) throws IllegalAccessException {
        field.setAccessible(true);
        Collection<?> collection = (Collection<?>) field.get(entity);
        if (collection != null) {
            for (Object object : collection) {
                if (object instanceof ModelEntity modelEntity) {
                    doRecursively(modelEntity, effect, touched);
                }
            }
        }
    }

}

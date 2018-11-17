package Trident.extensions.java.lang.Object;

import com.energyxxer.trident.compiler.util.Using;
import manifold.ext.api.Extension;
import manifold.ext.api.This;

@Extension
public class EObject {
    public static void ifNull(@This Object thiz, Runnable action) {
        if(thiz == null) action.run();
    }

    public static <T> Using<T> using( T object) {
        return new Using<>(object);
    }
}
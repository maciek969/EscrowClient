package elexis.modules.impl.render.javelin.target;

public enum Easing {
    LINEAR, EASE_OUT_EXPO;

    double apply(double progress) {
        return this == LINEAR || progress >= 1 ? progress : 1 - Math.pow(2, -10 * progress);
    }
}

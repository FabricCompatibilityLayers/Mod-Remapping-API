package fr.catcore.modremapperapi.remapping;

import java.util.Objects;

@Deprecated
public class VisitorInfos {

    @Deprecated
    public void registerSuperType(Type target, Type replacement) {
        throw new RuntimeException("This is not supposed to happen!");
    }

    @Deprecated
    public void registerTypeAnnotation(Type target, Type replacement) {
        throw new RuntimeException("This is not supposed to happen!");
    }

    @Deprecated
    public void registerMethodTypeIns(Type target, Type replacement) {
        throw new RuntimeException("This is not supposed to happen!");
    }

    @Deprecated
    public void registerMethodFieldIns(MethodNamed target, MethodNamed replacementObject) {
        throw new RuntimeException("This is not supposed to happen!");
    }

    @Deprecated
    public void registerMethodMethodIns(MethodNamed target, MethodNamed replacementObject) {
        throw new RuntimeException("This is not supposed to happen!");
    }

    @Deprecated
    public void registerMethodLdcIns(MethodValue target, MethodValue replacement) {
        throw new RuntimeException("This is not supposed to happen!");
    }

    @Deprecated
    public static class Type {
        public final String type;

        public Type(String type) {
            this.type = type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Type that = (Type) o;
            return type.equals(that.type);
        }
    }

    @Deprecated
    public static class MethodValue {
        public final String owner;
        public final Object value;

        public MethodValue(String owner, Object value) {
            this.owner = owner;
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MethodValue that = (MethodValue) o;
            return owner.equals(that.owner) && value.equals(that.value);
        }
    }

    @Deprecated
    public static class MethodNamed {
        public final String owner, name;

        public MethodNamed(String owner, String name) {
            this.owner = owner;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MethodNamed field = (MethodNamed) o;

            if (!Objects.equals(field.owner, this.owner)) return false;

            if (field.name.isEmpty() || this.name.isEmpty()) return true;

            return field.name.equals(this.name);
        }
    }
}

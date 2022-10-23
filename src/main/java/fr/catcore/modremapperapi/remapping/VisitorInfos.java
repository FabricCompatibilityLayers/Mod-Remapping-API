package fr.catcore.modremapperapi.remapping;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class VisitorInfos {
    protected final Map<Type, Type> SUPERS = new HashMap<>();
    protected final Map<Type, Type> ANNOTATION = new HashMap<>();
    protected final Map<Type, Type> METHOD_TYPE = new HashMap<>();
    protected final Map<MethodNamed, MethodNamed> METHOD_FIELD = new HashMap<>();
    protected final Map<MethodNamed, MethodNamed> METHOD_METHOD = new HashMap<>();
    protected final Map<MethodValue, MethodValue> METHOD_LDC = new HashMap<>();

    public void registerSuperType(Type methodType, Type methodType2) {
        SUPERS.put(methodType, methodType2);
    }

    public void registerTypeAnnotation(Type methodType, Type methodType2) {
        ANNOTATION.put(methodType, methodType2);
    }

    public void registerMethodTypeIns(Type methodType, Type methodType2) {
        METHOD_TYPE.put(methodType, methodType2);
    }

    public void registerMethodFieldIns(MethodNamed methodNamed, MethodNamed methodNamed2) {
        METHOD_FIELD.put(methodNamed, methodNamed2);
    }

    public void registerMethodMethodIns(MethodNamed methodNamed, MethodNamed methodNamed2) {
        METHOD_METHOD.put(methodNamed, methodNamed2);
    }

    public void registerMethodLdcIns(MethodValue methodValue, MethodValue methodValue2) {
        METHOD_LDC.put(methodValue, methodValue2);
    }

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

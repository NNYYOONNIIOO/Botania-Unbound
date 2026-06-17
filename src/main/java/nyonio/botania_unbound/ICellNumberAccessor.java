package nyonio.botania_unbound;

/**
 * Interface injected into TileCell via mixin to add number field access.
 */
public interface ICellNumberAccessor {
    int botania_unbound$getNumber();
    void botania_unbound$setNumber(int num);
}

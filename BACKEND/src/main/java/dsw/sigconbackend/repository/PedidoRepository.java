package dsw.sigconbackend.repository;

import dsw.sigconbackend.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findAllByOrderByFechaRegistroDesc();

    @Query(value = """
    SELECT COALESCE(MAX(
        CASE 
            WHEN codigo ~ '^PED-[0-9]{1,9}$' 
            THEN CAST(SUBSTRING(codigo, 5) AS INTEGER)
            ELSE 0
        END
    ), 0)
    FROM comercial.pedido
    WHERE codigo LIKE 'PED-%'
    """, nativeQuery = true)
Integer findMaxCodigoNumber();
}
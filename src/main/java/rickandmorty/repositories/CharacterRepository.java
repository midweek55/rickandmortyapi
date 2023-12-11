package rickandmorty.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import rickandmorty.models.CharacterEntity;

public interface CharacterRepository extends JpaRepository<CharacterEntity, Long> {

    boolean existsByName(String name);
}
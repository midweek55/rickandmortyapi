package rickandmorty.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rickandmorty.models.CharacterEntity;
import rickandmorty.services.CharacterAlreadyExistsException;
import rickandmorty.services.RickAndMortyService;

import java.util.List;

@RestController
public class RickAndMortyController {

    private final RickAndMortyService rickAndMortyService;

    @Autowired
    public RickAndMortyController(RickAndMortyService rickAndMortyService) {
        this.rickAndMortyService = rickAndMortyService;
    }

    @GetMapping("/api/characters")
    public List<CharacterEntity> getCharacters(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        return rickAndMortyService.getCharacters(page, pageSize);
    }

    @PostMapping("/api/characters")
    public ResponseEntity<String> saveCharacter(@RequestBody CharacterEntity characterEntity) {
        try {
            rickAndMortyService.saveCharacter(characterEntity);
            return new ResponseEntity<>("Personaje guardado exitosamente", HttpStatus.CREATED);
        } catch (CharacterAlreadyExistsException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

}

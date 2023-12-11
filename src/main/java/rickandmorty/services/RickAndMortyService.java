package rickandmorty.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import rickandmorty.models.CharacterEntity;
import rickandmorty.repositories.CharacterRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RickAndMortyService {

    @Value("${rickandmorty.api.url}/character")
    private String characterApiUrl;

    private final RestTemplate restTemplate;
    private final CharacterRepository characterRepository;

    public RickAndMortyService(RestTemplate restTemplate, CharacterRepository characterRepository) {
        this.restTemplate = restTemplate;
        this.characterRepository = characterRepository;
    }

    public CharacterEntity getCharacterByName(String name) {
        // Consulta a la API para obtener un personaje por nombre
        String url = String.format("%s?name=%s", characterApiUrl, name);
        ResponseEntity<ApiResponse<CharacterEntity>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ApiResponse<CharacterEntity>>() {}
        );

        if (response.getStatusCode().is2xxSuccessful()) {
            List<CharacterEntity> characterEntities = response.getBody().getResults();
            if (!characterEntities.isEmpty()) {
                return characterEntities.get(0);
            }
        }

        return null; // El personaje no fue encontrado en la API
    }

    @Transactional
    public void saveCharacter(CharacterEntity characterEntity) throws CharacterAlreadyExistsException {
        // Verificar si el personaje ya existe en la base de datos
        if (!isCharacterInDatabase(characterEntity)) {
            // Guardar el personaje en la base de datos
            saveCharacterToDatabase(characterEntity);
        } else {
            // El personaje ya existe en la base de datos
            throw new CharacterAlreadyExistsException("El personaje ya existe en la base de datos");
        }
    }

    private boolean isCharacterInDatabase(CharacterEntity characterEntity) {
        // Implementación para verificar si el personaje ya existe en la base de datos
        return characterRepository.existsByName(characterEntity.getName());
    }

    private void saveCharacterToDatabase(CharacterEntity character) {
        if (character.getName() != null && character.getImage() != null
                && character.getGender() != null && character.getStatus() != null) {
            CharacterEntity characterEntity = new CharacterEntity();
            characterEntity.setName(character.getName());
            characterEntity.setImage(character.getImage());
            characterEntity.setGender(character.getGender());
            characterEntity.setStatus(character.getStatus());

            characterRepository.save(characterEntity);

            System.out.println("Personaje guardado en la base de datos con ID: " + characterEntity.getId());
        } else {
            System.out.println("No se puede guardar el personaje en la base de datos debido a datos nulos.");
        }
    }

    public List<CharacterEntity> getCharacters(int page, int pageSize) {
        try {
            String urlWithPagination = String.format("%s?page=%d&pageSize=%d", characterApiUrl, page, pageSize);

            ResponseEntity<ApiResponse<CharacterEntity>> response = restTemplate.exchange(
                    urlWithPagination,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<ApiResponse<CharacterEntity>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                // Filtrar y combinar personajes de la API y la base de datos
                List<CharacterEntity> charactersFromApi = Objects.requireNonNull(response.getBody()).getResults();
                List<CharacterEntity> charactersFromDatabase = characterRepository.findAll().stream()
                        .map(characterEntity -> new CharacterEntity())
                        .collect(Collectors.toList());

                return mergeCharacters(charactersFromApi, charactersFromDatabase);
            } else {
                // Puedes lanzar una excepción o manejar el error según tus necesidades
                return Collections.emptyList();
            }
        } catch (Exception e) {
            // Manejar la excepción, por ejemplo, loguearla
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private List<CharacterEntity> mergeCharacters(List<CharacterEntity> charactersFromApi, List<CharacterEntity> charactersFromDatabase) {
        Map<String, CharacterEntity> characterMap = new HashMap<>();

        // Agregar personajes de la base de datos al mapa
        for (CharacterEntity databaseCharacterEntity : charactersFromDatabase) {
            characterMap.put(databaseCharacterEntity.getName(), databaseCharacterEntity);
        }

        // Agregar personajes de la API al mapa, evitando duplicados
        for (CharacterEntity apiCharacterEntity : charactersFromApi) {
            if (!characterMap.containsKey(apiCharacterEntity.getName())) {
                characterMap.put(apiCharacterEntity.getName(), apiCharacterEntity);
            }
        }

        // Crear una lista ordenada de personajes combinados
        List<CharacterEntity> mergedCharacterEntities = new ArrayList<>(characterMap.values());

        return mergedCharacterEntities;
    }

    private boolean containsName(List<CharacterEntity> characterEntities, String name) {
        // Verificar si la lista de personajes ya contiene un personaje con el mismo nombre
        return characterEntities.stream().anyMatch(characterEntity -> Objects.equals(characterEntity.getName(), name));
    }

    public static class ApiResponse<T> {
        private List<T> results;

        public List<T> getResults() {
            return results;
        }

        public void setResults(List<T> results) {
            this.results = results;
        }
    }
}
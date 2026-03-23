package fr.upjv.lesombresduson.data.model;

/**
 * Classe représentant un personnage du jeu.
 */
public class GameCharacter {
    private final int id;
    private final String name;
    private final int imageResId;

    /**
     * Constructeur de la classe GameCharacter.
     * @param id identifiant unique du personnage.
     * @param name nom du personnage.
     * @param imageResId identifiant de l'image du personnage.
     */
    public GameCharacter(int id, String name, int imageResId) {
        this.id = id;
        this.name = name;
        this.imageResId = imageResId;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public int getImageResId() { return imageResId; }
}
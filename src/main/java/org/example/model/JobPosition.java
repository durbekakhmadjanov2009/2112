package org.example.model;

public class JobPosition {
    private final int id;
    private final String name;
    private final String description;
    private final String requirements;

    public JobPosition(int id, String name, String description, String requirements) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.requirements = requirements;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getRequirements() { return requirements; }
}

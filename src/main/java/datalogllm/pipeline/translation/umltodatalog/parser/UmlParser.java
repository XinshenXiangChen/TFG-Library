package datalogllm.pipeline.translation.umltodatalog.parser;

import datalogllm.pipeline.umlMetamodel.UmlModel;

/**
 * Strategy interface for parsing a textual representation of a UML diagram
 * into a {@link UmlModel}. This allows for different parsing strategies
 * for various UML formats (e.g., PlantUML, Mermaid).
 */
public interface UmlParser {

    /**
     * Parses the given text representation of a UML diagram.
     *
     * @param umlText The textual representation of the UML diagram.
     * @return The parsed {@link UmlModel}.
     */
    UmlModel parse(String umlText);

    /** PlantUML class-diagram parser (default for IMP-GenLog). */
    static UmlParser plantUml() {
        return new PlantUmlParser();
    }

    static UmlParser defaultParser() {
        return plantUml();
    }
}
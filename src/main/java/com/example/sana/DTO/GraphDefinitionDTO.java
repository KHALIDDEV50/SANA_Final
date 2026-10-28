package com.example.sana.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

// Transfer format shared by graph snapshots and future AI proposals.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GraphDefinitionDTO {

    private List<NodeDTO> nodes;
    private List<EdgeDTO> edges;
    private Map<String, Object> properties;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NodeDTO {
        private String key;
        private Integer id;
        private String type;
        private Map<String, Object> properties;
        private Double positionX;
        private Double positionY;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EdgeDTO {
        private String key;
        private Integer id;
        private String type;
        // Stored separately from dataJson; do not overwrite a custom properties.label.
        private String label;
        private String sourceKey;
        private String targetKey;
        private Map<String, Object> properties;
    }
}

package com.micheanl.model.client.imgui;

import java.util.Objects;

public record MMDImGuiDrawData(
        int fontWidth,
        int fontHeight,
        int displayWidth,
        int displayHeight,
        Vertex[] vertices,
        int[] indices,
        Command[] commands
) {
    public MMDImGuiDrawData {
        vertices = vertices.clone();
        indices = indices.clone();
        commands = commands.clone();
        Objects.requireNonNull(vertices, "vertices");
        Objects.requireNonNull(indices, "indices");
        Objects.requireNonNull(commands, "commands");
    }

    public Expanded expand() {
        int total = 0;
        for (Command command : this.commands) {
            total = Math.addExact(total, command.elementCount());
        }
        Vertex[] expandedVertices = new Vertex[total];
        ExpandedCommand[] expandedCommands = new ExpandedCommand[this.commands.length];
        int vertexOffset = 0;
        for (int commandIndex = 0; commandIndex < this.commands.length; commandIndex++) {
            Command command = this.commands[commandIndex];
            int firstVertex = vertexOffset;
            for (int element = 0; element < command.elementCount(); element++) {
                int index = this.indices[command.indexOffset() + element] + command.vertexOffset();
                expandedVertices[vertexOffset++] = this.vertices[index];
            }
            expandedCommands[commandIndex] = new ExpandedCommand(
                    firstVertex,
                    command.elementCount(),
                    command.clipX0(),
                    command.clipY0(),
                    command.clipX1(),
                    command.clipY1()
            );
        }
        return new Expanded(expandedVertices, expandedCommands);
    }

    public record Vertex(float x, float y, float u, float v, int color) {
    }

    public record Command(int indexOffset, int elementCount, int vertexOffset, int clipX0, int clipY0, int clipX1, int clipY1) {
    }

    public record Expanded(Vertex[] vertices, ExpandedCommand[] commands) {
        public Expanded {
            vertices = vertices.clone();
            commands = commands.clone();
        }
    }

    public record ExpandedCommand(int vertexOffset, int vertexCount, int clipX0, int clipY0, int clipX1, int clipY1) {
    }
}

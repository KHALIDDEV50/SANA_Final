package com.example.sana.Service.CreativityDomainServices;

import com.example.sana.API.ApiException;
import com.example.sana.Service.DiagramServices.DiagramWriteGuard;
import com.example.sana.DTO.CreativityDomainDTOs.LocationDTO;
import com.example.sana.Model.CreativityDomainModels.Location;
import com.example.sana.Model.CreativityDomainModels.World;
import com.example.sana.Model.ProjectNode;
import com.example.sana.Model.ProjectOutput;
import com.example.sana.Repository.CreativityDomainRepository.LocationRepository;
import com.example.sana.Repository.CreativityDomainRepository.WorldRepository;
import com.example.sana.Repository.ProjectNodeRepository;
import com.example.sana.Repository.ProjectOutputRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository locationRepository;
    private final WorldRepository worldRepository;
    private final ProjectNodeRepository projectNodeRepository;
    private final ProjectOutputRepository projectOutputRepository;

    public List<Location> getAllLocations() {
        return locationRepository.findAll();
    }

    public Location getLocationById(Integer id) {
        Location location = locationRepository.findLocationById(id);

        if (location == null) {
            throw new ApiException("Location not found");
        }

        return location;
    }


    @Transactional
    public void addLocation(Integer outputId, Integer worldId, Integer parentLocationId, LocationDTO dto) {

        ProjectOutput output = projectOutputRepository.findProjectOutputById(outputId);
        DiagramWriteGuard.rejectUnversionedWrite(output);

        if (output == null) {
            throw new ApiException("Project output not found");
        }

        World world = null;
        Location parentLocation = null;

        // Check World only if worldId is provided
        if (worldId != null) {
            world = worldRepository.findWorldById(worldId);

            if (world == null) {
                throw new ApiException("World not found");
            }

            if (!world.getProjectNode().getProjectOutput().getId().equals(outputId)) {
                throw new ApiException("World does not belong to this project output");
            }
        }

        // Check Parent Location only if parentLocationId is provided
        if (parentLocationId != null) {
            parentLocation = locationRepository.findLocationById(parentLocationId);

            if (parentLocation == null) {
                throw new ApiException("Parent location not found");
            }

            if (!parentLocation.getProjectNode()
                    .getProjectOutput().getId().equals(outputId)) {
                throw new ApiException("Parent location does not belong to this project output");
            }

            if (world != null) {
                if (parentLocation.getWorld() == null || !parentLocation.getWorld().getId().equals(world.getId())) {
                    throw new ApiException("Parent location does not belong to this world");
                }
            } else {
                world = parentLocation.getWorld();
            }
        }

        // Create ProjectNode
        ProjectNode node = new ProjectNode();
        node.setType("LOCATION");
        node.setPositionX(dto.getPositionX());
        node.setPositionY(dto.getPositionY());
        node.setProjectOutput(output);
        node.setCreatedAt(LocalDate.now());
        node.setUpdatedAt(LocalDate.now());

        // Create Location
        Location location = new Location();
        location.setName(dto.getName());
        location.setLocationType(dto.getLocationType());
        location.setNarrativeSignificance(dto.getNarrativeSignificance());
        location.setNotes(dto.getNotes());
        location.setCreatedAt(LocalDate.now());
        location.setUpdateAt(LocalDate.now());

        location.setWorld(world);
        location.setParentLocation(parentLocation);

        // Link both sides
        location.setProjectNode(node);
        node.setLocation(location);

        projectNodeRepository.save(node);
    }


    @Transactional
    public void updateLocation(Integer id, LocationDTO dto) {

        Location location = locationRepository.findLocationById(id);
        DiagramWriteGuard.rejectUnversionedWrite(location == null || location.getProjectNode() == null ? null : location.getProjectNode().getProjectOutput());

        if (location == null) {
            throw new ApiException("Location not found");
        }

        ProjectNode node = location.getProjectNode();

        if (node == null) {
            throw new ApiException("Project node not found");
        }

        // Update Location
        location.setName(dto.getName());
        location.setLocationType(dto.getLocationType());
        location.setNarrativeSignificance(dto.getNarrativeSignificance());
        location.setNotes(dto.getNotes());
        location.setUpdateAt(LocalDate.now());

        // Update existing ProjectNode
        node.setPositionX(dto.getPositionX());
        node.setPositionY(dto.getPositionY());
        node.setUpdatedAt(LocalDate.now());

        locationRepository.save(location);
        projectNodeRepository.save(node);
    }

    @Transactional
    public void updateLocationWorld(Integer locationId, Integer worldId) {

        Location location = locationRepository.findLocationById(locationId);
        DiagramWriteGuard.rejectUnversionedWrite(location == null || location.getProjectNode() == null ? null : location.getProjectNode().getProjectOutput());

        if (location == null) {
            throw new ApiException("Location not found");
        }

        World world = worldRepository.findWorldById(worldId);

        if (world == null) {
            throw new ApiException("World not found");
        }

        if (!location.getProjectNode().getProjectOutput().getId().equals(world.getProjectNode().getProjectOutput().getId())) {
            throw new ApiException("Location and world must belong to the same output");
        }

        if (location.getParentLocation() != null && (location.getParentLocation().getWorld() == null || !location.getParentLocation().getWorld().getId().equals(worldId))) {
            throw new ApiException("Parent location belongs to a different world");
        }

        if (location.getChildLocations() != null) {
            for (Location child : location.getChildLocations()) {
                if (child.getWorld() == null || !child.getWorld().getId().equals(worldId)) {
                    throw new ApiException("Child locations belong to a different world");
                }
            }
        }

        location.setWorld(world);
        location.setUpdateAt(LocalDate.now());
        locationRepository.save(location);
    }

    @Transactional
    public void updateParentLocation(Integer locationId, Integer parentLocationId) {

        Location location = locationRepository.findLocationById(locationId);
        DiagramWriteGuard.rejectUnversionedWrite(location == null || location.getProjectNode() == null ? null : location.getProjectNode().getProjectOutput());

        if (location == null) {
            throw new ApiException("Location not found");
        }

        Location parentLocation = locationRepository.findLocationById(parentLocationId);

        if (parentLocation == null) {
            throw new ApiException("Parent location not found");
        }

        if (locationId.equals(parentLocationId)) {
            throw new ApiException("Location cannot be its own parent");
        }

        if (!location.getProjectNode().getProjectOutput().getId().equals(parentLocation.getProjectNode().getProjectOutput().getId())) {
            throw new ApiException("Locations must belong to the same project output");
        }

        // Prevent circular relationships
        Location current = parentLocation;

        while (current != null) {
            if (current.getId().equals(locationId)) {
                throw new ApiException("Circular parent location is not allowed");
            }
            current = current.getParentLocation();
        }

        if (location.getWorld() != null && (parentLocation.getWorld() == null || !location.getWorld().getId().equals(parentLocation.getWorld().getId()))) {
            throw new ApiException("Location and parent must belong to the same world");
        }

        if (location.getWorld() == null) {
            if (location.getChildLocations() != null && !location.getChildLocations().isEmpty()) {
                for (Location child : location.getChildLocations()) {
                    if (child.getWorld() != null && (parentLocation.getWorld() == null || !child.getWorld().getId().equals(parentLocation.getWorld().getId()))) {
                        throw new ApiException("Child location belongs to a different world");
                    }
                }
            }

            location.setWorld(parentLocation.getWorld());
        }

        location.setParentLocation(parentLocation);
        location.setUpdateAt(LocalDate.now());

        locationRepository.save(location);
    }

    @Transactional
    public void deleteLocation(Integer id) {

        Location location = locationRepository.findLocationById(id);
        DiagramWriteGuard.rejectUnversionedWrite(location == null || location.getProjectNode() == null ? null : location.getProjectNode().getProjectOutput());

        if (location == null) {
            throw new ApiException("Location not found");
        }

        if (location.getChildLocations() != null && !location.getChildLocations().isEmpty()) {
            throw new ApiException("Cannot delete a location that has child locations");
        }

        ProjectNode node = location.getProjectNode();

        if (node == null) {
            locationRepository.delete(location);
            return;
        }

        projectNodeRepository.delete(node);
    }
}


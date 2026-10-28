package com.example.sana.Controller.CreativityDomainController;

import com.example.sana.Service.OwnershipService;
import com.example.sana.Model.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.example.sana.API.ApiResponse;
import com.example.sana.DTO.CreativityDomainDTOs.LocationDTO;
import com.example.sana.Service.CreativityDomainServices.LocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/location")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;
    private final OwnershipService ownershipService;


    // Get all locations in the system
    @GetMapping("/get")
    public ResponseEntity<?> getAllLocations() {
        return ResponseEntity.status(200).body(locationService.getAllLocations());
    }


    // Get a specific location by its ID
    @GetMapping("/get/{id}")
    public ResponseEntity<?> getLocationById(@AuthenticationPrincipal User user, @PathVariable Integer id) {
        ownershipService.checkLocation(user, id);

        return ResponseEntity.status(200).body(locationService.getLocationById(id));
    }


    // Add a new standalone location without a World or Parent Location
    // The backend automatically creates and links a ProjectNode
    // outputId = the ProjectOutput where the location will be created
    @PostMapping("/add/{outputId}")
    public ResponseEntity<?> addLocation(@AuthenticationPrincipal User user, @PathVariable Integer outputId, @RequestBody @Valid LocationDTO dto) {
        ownershipService.checkOutput(user, outputId);

        locationService.addLocation(outputId, null, null, dto);
        return ResponseEntity.status(200).body(new ApiResponse("Location added successfully"));
    }


    // Add a new location inside an existing World
    // The location will NOT have a Parent Location
    // outputId = the ProjectOutput ID
    // worldId = the World ID
    @PostMapping("/add/{outputId}/world/{worldId}")
    public ResponseEntity<?> addLocationToWorld(@AuthenticationPrincipal User user, @PathVariable Integer outputId, @PathVariable Integer worldId, @RequestBody @Valid LocationDTO dto) {
        ownershipService.checkOutput(user, outputId);

        locationService.addLocation(outputId, worldId, null, dto);
        return ResponseEntity.status(200).body(new ApiResponse("Location added to world successfully"));
    }


    // Add a new location inside another Location (Parent Location)
    // Example: Adding a Palace inside a City
    // The World is automatically inherited from the Parent Location
    // outputId = the ProjectOutput ID
    // parentLocationId = the existing Parent Location ID
    @PostMapping("/add/{outputId}/parent/{parentLocationId}")
    public ResponseEntity<?> addChildLocation(@AuthenticationPrincipal User user, @PathVariable Integer outputId, @PathVariable Integer parentLocationId, @RequestBody @Valid LocationDTO dto) {
        ownershipService.checkOutput(user, outputId);

        locationService.addLocation(outputId, null, parentLocationId, dto);
        return ResponseEntity.status(200).body(new ApiResponse("Child location added successfully"));
    }


    // Add a new location with BOTH a World and a Parent Location
    // Example: Adding a Palace inside a City that belongs to World Aetheria
    // The backend verifies that the Parent Location belongs to the specified World
    // outputId = the ProjectOutput ID
    // worldId = the World ID
    // parentLocationId = the Parent Location ID
    @PostMapping("/add/{outputId}/world/{worldId}/parent/{parentLocationId}")
    public ResponseEntity<?> addLocationToWorldAndParent(@AuthenticationPrincipal User user, @PathVariable Integer outputId, @PathVariable Integer worldId, @PathVariable Integer parentLocationId, @RequestBody @Valid LocationDTO dto) {
        ownershipService.checkOutput(user, outputId);

        locationService.addLocation(outputId, worldId, parentLocationId, dto);

        return ResponseEntity.status(200).body(new ApiResponse("Child location added successfully"));
    }


    // Update a Location's information and its ProjectNode position
    // Updates: name, locationType, narrativeSignificance, notes, positionX, positionY
    // Does NOT change the World or Parent Location relationships
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateLocation(@AuthenticationPrincipal User user, @PathVariable Integer id, @RequestBody @Valid LocationDTO dto) {
        ownershipService.checkLocation(user, id);

        locationService.updateLocation(id, dto);
        return ResponseEntity.status(200).body(new ApiResponse("Location updated successfully"));
    }


    // Change the World associated with an existing Location
    // locationId = the Location to update
    // worldId = the new World to assign
    // The backend checks that the World belongs to the same ProjectOutput
    @PutMapping("/update/{locationId}/world/{worldId}")
    public ResponseEntity<?> updateLocationWorld(@AuthenticationPrincipal User user, @PathVariable Integer locationId, @PathVariable Integer worldId) {
        ownershipService.checkLocation(user, locationId);

        locationService.updateLocationWorld(locationId, worldId);

        return ResponseEntity.status(200).body(new ApiResponse("Location world updated successfully"));
    }


    // Change the Parent Location of an existing Location
    // Example: Moving a Palace from one City to another
    // The backend prevents a Location from becoming its own parent
    // and prevents circular parent-child relationships
    @PutMapping("/update/{locationId}/parent/{parentLocationId}")
    public ResponseEntity<?> updateParentLocation(@AuthenticationPrincipal User user, @PathVariable Integer locationId, @PathVariable Integer parentLocationId) {
        ownershipService.checkLocation(user, locationId);

        locationService.updateParentLocation(locationId, parentLocationId);

        return ResponseEntity.status(200).body(new ApiResponse("Parent location updated successfully"));
    }


    // Delete a Location and its associated ProjectNode
    // A Location cannot be deleted if it contains Child Locations
    // Deleting the Node may also delete its connected ProjectEdges
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteLocation(@AuthenticationPrincipal User user, @PathVariable Integer id) {
        ownershipService.checkLocation(user, id);

        locationService.deleteLocation(id);

        return ResponseEntity.status(200).body(new ApiResponse("Location deleted successfully"));
    }
}

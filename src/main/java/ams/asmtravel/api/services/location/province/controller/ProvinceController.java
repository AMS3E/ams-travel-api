package ams.asmtravel.api.services.location.province.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import ams.asmtravel.api.services.location.province.dto.request.ProvinceRequest;
import ams.asmtravel.api.services.location.province.dto.response.ProvinceResponse;
import ams.asmtravel.api.services.location.province.service.ProvinceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Cambodia's provinces, each in one tourism zone. */
@Tag(name = "Discover · Provinces", description = "Cambodia's provinces, each in one zone.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/provinces")
public class ProvinceController {

    private final ProvinceService provinceService;

    @GetMapping
    public List<ProvinceResponse> getAll() {
        return provinceService.getAll();
    }

    @GetMapping("/{id}")
    public ProvinceResponse getById(@PathVariable UUID id) {
        return provinceService.findById(id);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ProvinceResponse create(@Valid @RequestBody ProvinceRequest request) {
        return provinceService.create(request);
    }

    @PutMapping("/{id}")
    public ProvinceResponse update(@PathVariable UUID id, @Valid @RequestBody ProvinceRequest request) {
        return provinceService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        provinceService.delete(id);
    }
}

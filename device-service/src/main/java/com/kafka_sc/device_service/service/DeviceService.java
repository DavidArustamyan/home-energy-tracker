package com.kafka_sc.device_service.service;

import com.kafka_sc.device_service.dto.DeviceDto;
import com.kafka_sc.device_service.entity.Device;
import com.kafka_sc.device_service.exception.DeviceNotFoundException;
import com.kafka_sc.device_service.repository.DeviceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;

    public DeviceService(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    public DeviceDto getDeviceById(Long id) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new DeviceNotFoundException(id));
        return toDto(device);
    }

    public DeviceDto createDevice(DeviceDto input) {
        Device device = new Device();
        device.setName(input.getName());
        device.setLocation(input.getLocation());
        device.setType(input.getType());
        device.setUserId(input.getUserId());

        final Device savedDevice = deviceRepository.save(device);
        return toDto(savedDevice);
    }

    public DeviceDto updateDevice(Long id, DeviceDto input) {
        Device existing = deviceRepository.findById(id)
                .orElseThrow(()-> new DeviceNotFoundException(id));
        existing.setName(input.getName());
        existing.setType(input.getType());
        existing.setLocation(input.getLocation());
        existing.setUserId(input.getUserId());

        final Device updatedDevice = deviceRepository.save(existing);
        return toDto(updatedDevice);
    }

    public void deleteDevice(Long id) {
        if (!deviceRepository.existsById(id)) {
            throw new DeviceNotFoundException(id);
        }
        deviceRepository.deleteById(id);
    }

    public List<DeviceDto> getAllDevicesByUserId(Long userId) {
        List<Device> devices = deviceRepository.findAllByUserId(userId);
        return devices.stream()
                .map(this::toDto)
                .toList();
    }

    private DeviceDto toDto(Device device) {
        DeviceDto dto = new DeviceDto();
        dto.setId(device.getId());
        dto.setName(device.getName());
        dto.setLocation(device.getLocation());
        dto.setType(device.getType());
        dto.setUserId(device.getUserId());
        return dto;
    }
}

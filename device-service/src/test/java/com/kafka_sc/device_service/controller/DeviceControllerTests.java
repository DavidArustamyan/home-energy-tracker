package com.kafka_sc.device_service.controller;

import com.kafka_sc.device_service.dto.DeviceDto;
import com.kafka_sc.device_service.exception.DeviceNotFoundException;
import com.kafka_sc.device_service.model.DeviceType;
import com.kafka_sc.device_service.service.DeviceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.BDDMockito.willThrow;

@WebMvcTest(DeviceController.class)
class DeviceControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeviceService deviceService;

    @Test
    void getDevice_returnsDevice() throws Exception {
        given(deviceService.getDeviceById(1L)).willReturn(device(1L));

        mockMvc.perform(get("/api/v1/device/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.type").value("LIGHT"));
    }

    @Test
    void getDevice_whenMissing_returnsProblemDetail() throws Exception {
        given(deviceService.getDeviceById(99L)).willThrow(new DeviceNotFoundException(99L));

        mockMvc.perform(get("/api/v1/device/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Device not found"))
                .andExpect(jsonPath("$.detail").value("Device with id 99 not found"))
                .andExpect(jsonPath("$.instance").value("/api/v1/device/99"));
    }

    @Test
    void createDevice_returns201WithLocation() throws Exception {
        given(deviceService.createDevice(any())).willReturn(device(42L));

        mockMvc.perform(post("/api/v1/device")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                            {"name":"Kitchen light","type":"LIGHT","location":"Kitchen","userId":1}
                        """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/device/42"))
                .andExpect(jsonPath("$.id").value(42));
    }

    @Test
    void createDevice_withBadJson_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/device")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{xyz"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void createDevice_withUnknownType_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/device")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                            {"name":"MachineALaver","type":"FRIGO","location":"Kitchen","userId":1}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteDevice_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/device/5"))
                .andExpect(status().isNoContent());
        verify(deviceService).deleteDevice(5L);
    }

    @Test
    void deleteDevice_whenMissing_returns404() throws Exception {
        willThrow(new DeviceNotFoundException(7L)).given(deviceService).deleteDevice(7L);

        mockMvc.perform(delete("/api/v1/device/7"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Device not found"));
    }

    private static DeviceDto device(Long id) {
        return DeviceDto.builder()
                .id(id)
                .name("Kitchen light")
                .type(DeviceType.LIGHT)
                .location("Kitchen")
                .userId(1L)
                .build();
    }

}

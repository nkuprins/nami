package com.app.backend.dto;

import java.util.List;

public record PresignRequest(List<String> filenames) {}

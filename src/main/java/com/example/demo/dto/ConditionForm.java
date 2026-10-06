package com.example.demo.dto;
import java.util.Set;
/** Seller declaration, not a certification. Null legacy form preserves existing values. */
public record ConditionForm(String appearance, String operation, String defects, String repair,
                            String repairDetails, String accessories, Set<Long> defectAssets, Set<Integer> defectIndexes) { }

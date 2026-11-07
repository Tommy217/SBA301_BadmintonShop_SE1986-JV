package fu.se.badmintonshop.modules.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import fu.se.badmintonshop.modules.auth.entity.Address;
import lombok.*;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressResponse {

    private Long id;
    private Long userId;
    private String receiverName;
    private String phone;
    private String province;
    private String district;
    private String ward;
    private String street;

    @JsonProperty("isDefault")
    private boolean isDefault;

    private String fullAddress;

    public static AddressResponse fromEntity(Address address) {
        if (address == null) {
            return null;
        }

        String full = Stream.of(address.getStreet(), address.getWard(), address.getDistrict(), address.getProvince())
            .filter(part -> part != null && !part.isBlank())
            .map(String::trim)
            .collect(Collectors.joining(", "));

        return AddressResponse.builder()
            .id(address.getId())
            .userId(address.getUser() != null ? address.getUser().getId() : null)
            .receiverName(address.getReceiverName())
            .phone(address.getPhone())
            .province(address.getProvince())
            .district(address.getDistrict())
            .ward(address.getWard())
            .street(address.getStreet())
            .isDefault(address.isDefault())
            .fullAddress(full)
            .build();
    }
}

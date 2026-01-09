package Jaksim.jaksim_server.domain.user.model;

import Jaksim.jaksim_server.global.jpa.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;


@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "users",
        uniqueConstraints = @UniqueConstraint(name = "uk_users_device_id", columnNames = "device_id"))
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "device_id", nullable = false, length = 64)
    private String deviceId;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    //TODO : 사용자가 배지를 가지고 있지 않다. 현재는 서버에 배지가 직접 저장되어서, home으로 쏴주는중. 사용자 거치지 않고.


    @Builder
    private User(String deviceId) {
        this.deviceId = deviceId;
        this.isActive = true;
    }

    public static User create(String deviceId) {
        return User.builder().deviceId(deviceId).build();
    }

    public void deactivate() {
        this.isActive = false;
    }
}

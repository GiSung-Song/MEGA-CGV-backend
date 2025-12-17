import http from "k6/http"
import { check } from "k6"

export const options = {
    vus: 20,
    iterations: 20
}

const SEAT_SCENARIOS = [
    [180],
    [179, 180],
    [180, 181],
    [178, 180, 182],
    [180, 183],
    [180, 184],
    [180, 185],
    [180, 186],
    [180, 187],
    [180, 188],
    [190],
    [192],
]

export function setup() {
    const BASE_URL = "http://host.docker.internal:8080";

    const tokens = [];

    for (let i = 1; i <= 20; i++) {
        const email = `asdfqwer${i}@email.com`;
        const password = "asdf1234!@";
        const phoneNumber = `0109000${String(i).padStart(4, "0")}`;

        /* 회원가입
        http.post(
            `${BASE_URL}/api/users`,
            JSON.stringify({
                name: `이름${i}`,
                email: email,
                password: password,
                phoneNumber: phoneNumber
            }),
            { headers: { "Content-Type": "application/json"}}
        );
         */

        const loginRes = http.post(
            `${BASE_URL}/api/auth/login`,
            JSON.stringify({
                email: email,
                password: password
            }),
            { headers: { "Content-Type": "application/json"}}
        );

        const accessToken = loginRes.json("data.accessToken");
        tokens.push(accessToken);
    }

    return { tokens };
}

export default function (data) {
    const BASE_URL = "http://host.docker.internal:8080";
    const url = `${BASE_URL}/api/screenings/5/seats/hold`;

    const userIndex = __VU - 1;
    const token = data.tokens[userIndex];

    const seatIds = SEAT_SCENARIOS[userIndex % SEAT_SCENARIOS.length];

    const res = http.post(
        url,
        JSON.stringify({
            screeningSeatIds: seatIds
        }),
        {
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${token}`,
            }
        }
    );

    check(res, {
        "status is 200 or 409": (r) =>
            r.status === 200 || r.status === 409
    });
}

// docker 실행
// docker run --rm -i -v ${PWD}:/scripts grafana/k6 run /scripts/k6/seat_hold_test.js
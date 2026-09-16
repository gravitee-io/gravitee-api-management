/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { Navigate, Outlet } from 'react-router-dom';

import { useRegistrationEnabled } from '../auth.selectors';

/**
 * Guards the sign-up request page only. The activation route must stay outside it: someone
 * holding a valid token deserves the server's explanation, which comes from finalization,
 * not a silent bounce to a login page that says nothing.
 */
export function RegistrationEnabledRoute() {
    const registrationEnabled = useRegistrationEnabled();

    if (!registrationEnabled) {
        return <Navigate to="/login" replace />;
    }

    return <Outlet />;
}

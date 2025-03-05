import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export interface Spec extends TurboModule {
  getOtp(otpLength: number, phoneNumber?: string): Promise<string>;
  getSms(phoneNumber?: string): Promise<string>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('RnAndroidSmsRetriever');

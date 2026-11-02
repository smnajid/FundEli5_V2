export * from './answerController.service';
import { AnswerControllerService } from './answerController.service';
export * from './checkRecordController.service';
import { CheckRecordControllerService } from './checkRecordController.service';
export * from './validationController.service';
import { ValidationControllerService } from './validationController.service';
export const APIS = [AnswerControllerService, CheckRecordControllerService, ValidationControllerService];
